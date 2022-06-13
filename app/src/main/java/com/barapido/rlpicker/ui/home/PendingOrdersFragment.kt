package com.barapido.rlpicker.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.barapido.rlpicker.R
import com.barapido.rlpicker.api.Rubian
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception

private const val TAG = "PendingOrdersFragment"
private var orders = JSONArray()
private lateinit var ordersAdapter: OrdersListAdapter
private lateinit var recyclerView: RecyclerView
private var loading = true
private var current_page = 0
private var page_size = 100

class PendingOrdersFragment: Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        auth = FirebaseAuth.getInstance()
        email = auth.currentUser!!.email.toString()

        orders = JSONArray()
        current_page = 0
        getOrders()

        val root = inflater.inflate(R.layout.recycler_order_list, container, false)
        val ordersManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
        ordersAdapter = OrdersListAdapter(orders, onItemClicked = { position -> onListItemClick(position) } )
        recyclerView =  root.findViewById(R.id.order_list)
        recyclerView.layoutManager = ordersManager
        recyclerView.adapter = ordersAdapter
        recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    val visibleItemCount = ordersManager.childCount
                    val totalItemCount = ordersManager.itemCount
                    val pastVisibleItems = (ordersManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    if (loading) {
                        if (visibleItemCount + pastVisibleItems >= totalItemCount) {
                            loading = false
                            Log.d(TAG, "last item")
                            current_page += page_size
                            getPaginatedOrders(current_page)
                            loading = true
                        }
                    }
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        })
        recyclerView.setHasFixedSize(true)

        return root
    }

    private fun onListItemClick(position: Int) {
        if (orders.length() > 0) {
            val builder = AlertDialog.Builder(requireActivity())
            builder.setTitle("Assign to Me")
            builder.setMessage("Are you sure you want to take this order?")
            builder.setPositiveButton(R.string.confirm) { dialog, which ->
                assignPicker(orders.getJSONObject(position))
                requireActivity().findViewById<ViewPager>(R.id.tabs_pager).currentItem = 1
            }
            builder.setNegativeButton(R.string.cancel) { dialog, which -> }
            builder.show()
        }

    }

    fun assignPicker(obj: JSONObject) {
        val route = "/resource/Pick List/${obj.getString("name")}"
        var req = object: JsonObjectRequest(
            Request.Method.GET, Rubian.BASE_URL + route, null, {   piRes ->
                val pi = piRes.getJSONObject("data")
                if (pi.has("picker") && !pi.getString("picker").isNullOrEmpty()) {
                    Toast.makeText(context, "${pi.getString("picker")} is already assigned to the picklist", Toast.LENGTH_LONG).show()
                } else {
                    val route = "/resource/Pick List/${obj.getString("name")}"
                    var req = object: JsonObjectRequest(
                        Request.Method.PUT, Rubian.BASE_URL + route, JSONObject("{'picker': ${email}, 'picking_status': 'ongoing'}"), {   it
                        },
                        { err -> Log.d(TAG, err.toString())}
                    ) {
                        override fun getHeaders(): MutableMap<String, String> {
                            val headers = HashMap<String, String>()
                            headers["Authorization"] = "token ${Rubian.ACCESS_KEY}"
                            return headers
                        }
                    }
                    Rubian.getInstance(requireActivity()).addToRequestQueue(req)
                }
            },
            { err -> Log.d(TAG, err.toString())}
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "token ${Rubian.ACCESS_KEY}"
                return headers
            }
        }
        Rubian.getInstance(requireActivity()).addToRequestQueue(req)

    }

    private var pickListLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        orders = JSONArray()
        current_page = 0
        getOrders()
    }

    private fun getOrders() {
        val route = "/resource/Pick List?fields=[\"name\",\"sales_order\",\"required_delivery_date\",\"customer\",\"store_name\",\"address_city\"]&filters=[[\"docstatus\",\"=\",0],[\"picker\",\"=\",\"\"]]&order_by=required_delivery_date%20asc,address_city&limit_page_length="+page_size
        var req = object: JsonObjectRequest(
            Request.Method.GET, Rubian.BASE_URL + route, null, { res ->
                val items = res.getJSONArray("data")
                for(i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    orders.put(item)
                }
                ordersAdapter = OrdersListAdapter(orders, onItemClicked = { position -> onListItemClick(position) } )
                //ordersAdapter.notifyDataSetChanged()
                recyclerView.adapter = ordersAdapter
            },
            { err -> Log.d(TAG, err.toString())}
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "token ${Rubian.ACCESS_KEY}"
                return headers
            }
        }
        Rubian.getInstance(requireActivity()).addToRequestQueue(req)
    }


    private fun getPaginatedOrders(page: Int) {
        Log.d(TAG, "page: " + page);
        val route = "/resource/Pick List?fields=[\"name\",\"sales_order\",\"required_delivery_date\",\"customer\",\"store_name\",\"address_city\"]&filters=[[\"docstatus\",\"=\",0],[\"picker\",\"=\",\"\"]]&order_by=required_delivery_date%20asc,address_city&limit_page_length="+page_size+"&limit_start=" + page
        var req = object: JsonObjectRequest(
            Request.Method.GET, Rubian.BASE_URL + route, null, { res ->
                val items = res.getJSONArray("data")
                for(i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    orders.put(item)
                }
                //ordersAdapter = OrdersListAdapter(orders, onItemClicked = { position -> onListItemClick(position) } )
                ordersAdapter.notifyDataSetChanged()
                //recyclerView.adapter = ordersAdapter
            },
            { err -> Log.d(TAG, err.toString())}
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "token ${Rubian.ACCESS_KEY}"
                return headers
            }
        }
        Rubian.getInstance(requireActivity()).addToRequestQueue(req)
    }


    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        try {
            if (isVisibleToUser) {
                orders = JSONArray()
                current_page = 0
                getOrders()
            }
        } catch(e: Exception) {

        }
    }
}