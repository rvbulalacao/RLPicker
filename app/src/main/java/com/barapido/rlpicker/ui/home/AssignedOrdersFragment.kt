package com.barapido.rlpicker.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.barapido.rlpicker.R
import com.barapido.rlpicker.api.Rubian
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray

private const val TAG = "AssignedOrdersFragment"
private var orders = JSONArray()
private lateinit var ordersAdapter: OrdersListAdapter
private lateinit var recyclerView: RecyclerView

private var loading = true
private var current_page = 0
private var page_size = 100

class AssignedOrdersFragment: Fragment() {

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
                            // nothing happens
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
            val intent = Intent(context, OrderActivity::class.java).apply {
                putExtra("picklist", orders.getJSONObject(position).getString("name"))
            }
            pickListLauncher.launch(intent)
        }

    }

    private var pickListLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        orders = JSONArray()
        getOrders()
    }

    fun getOrders() {
        val route = "/resource/Pick List?fields=[\"name\",\"sales_order\",\"required_delivery_date\",\"customer\",\"store_name\",\"address_city\"]&filters=[[\"docstatus\",\"=\",0],[\"picking_status\",\"=\",\"ongoing\"],[\"picker\",\"=\",\"${email}\"]]&limit_page_length=None"
        var req = object: JsonObjectRequest(
            Request.Method.GET, Rubian.BASE_URL + route, null, { res ->
                Log.d(TAG, res.toString())
                val items = res.getJSONArray("data")
                for(i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    orders.put(item)
                }
                ordersAdapter = OrdersListAdapter(orders, onItemClicked = { position -> onListItemClick(position) } )
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
        Rubian.getInstance(requireContext()).addToRequestQueue(req)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        try {
            if (isVisibleToUser) {
                orders = JSONArray()
                getOrders()
            }
        } catch (e: Exception) {

        }

    }

}