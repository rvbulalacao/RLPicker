package com.barapido.rlpicker.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.barapido.rlpicker.R
import com.barapido.rlpicker.Scanner
import com.barapido.rlpicker.api.Rubian
import com.barapido.rlpicker.uihome.PicklistAdapter
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

private const val TAG = "OrderActivity"

private var items = JSONArray()

private lateinit var picklistAdapter: PicklistAdapter

private lateinit var salesOrder: TextView
private lateinit var customer: TextView
private lateinit var requiredDeliverySchedule: TextView
private lateinit var notes: TextView
private lateinit var qtyOrdered: TextView
private lateinit var qtyPicked: TextView
//private lateinit var qtyConfirmed: TextView

private lateinit var finalizeBtn: Button
private lateinit var scanBtn: Button

private lateinit var recyclerView: RecyclerView

class OrderActivity : AppCompatActivity() {

    private lateinit var picklist: String
    private lateinit var posBarcode: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)

        picklist = intent.getStringExtra("picklist").toString()

        getPicklist(picklist)

        finalizeBtn = findViewById(R.id.finalize_btn)
        scanBtn = findViewById(R.id.scan_btn)

        salesOrder = findViewById(R.id.sales_order)
        customer = findViewById(R.id.customer)
        requiredDeliverySchedule = findViewById(R.id.required_delivery_date)
        notes = findViewById(R.id.notes)
        qtyOrdered = findViewById(R.id.qty_ordered)
        qtyPicked = findViewById(R.id.qty_picked)
        //qtyConfirmed = findViewById(R.id.qty_confirmed)


        val picklistManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        picklistAdapter = PicklistAdapter(items)
        recyclerView = findViewById(R.id.picklist)

        posBarcode = findViewById(R.id.pos_barcode)
        posBarcode.setOnKeyListener(object : View.OnKeyListener {
                override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                    Log.d(TAG, keyCode.toString())
                    if (event!!.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER
                    ) {
                        hideSoftKeyboard()
                        posBarcode.clearFocus()
                        posBarcode.isCursorVisible = false

                        if (posBarcode.text.toString().isNotEmpty()) {
                            val scanIntent = Intent(baseContext, ItemActivity::class.java).apply {
                                var found = false
                                for(i in 0 until items.length()) {
                                    val item = items.getJSONObject(i)
                                    if (item.getString("barcode").trimStart('0').trim() == posBarcode.text.toString().trimStart('0').trim()
                                        && item.getInt("actual_picked_qty") == 0) {
                                        putExtra("item", item.toString())
                                        pickingLauncher.launch(this)
                                        found = true
                                        break;
                                    }
                                }
                                if (!found) {
                                    Toast.makeText(baseContext, "Item ${posBarcode.text.toString()} not found", Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        return true
                    }
                    return false
                }

            });
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        recyclerView.layoutManager = picklistManager
        recyclerView.adapter = picklistAdapter
        recyclerView.setHasFixedSize(true)
    }

    private fun getPicklist(id: String) {
        items = JSONArray()
        val route = "/resource/Pick List/${id}"
        var req = object: JsonObjectRequest(
            Request.Method.GET, Rubian.BASE_URL + route, null, { res ->
                val obj = res.getJSONObject("data");
                salesOrder.text = obj.getString("sales_order").substring(8)
                customer.text = obj.getString("store_name")
                requiredDeliverySchedule.text = obj.getString("required_delivery_date")
                try {
                    notes.text = obj.getString("special_instructions")
                } catch (e: Exception) { }

                val status = obj.getString("picking_status")
                if (status == "done") {
                    finalizeBtn.visibility = View.INVISIBLE
                    scanBtn.visibility = View.INVISIBLE
                } else {
                    finalizeBtn.visibility = View.VISIBLE
                }
                var totalOrdered = 0
                var totalPicked = 0
                //var totalConfirmed = 0
                val locations = obj.getJSONArray("locations");
                for(i in 0 until locations.length()) {
                    val location = locations.getJSONObject(i)
                    totalOrdered += location.getInt("qty")
                    totalPicked += location.getInt("unconfirmed_picked_qty")
                    //totalConfirmed += location.getInt("confirmed_picked_qty")
                    items.put(location)
                }
                qtyOrdered.text = totalOrdered.toString()
                qtyPicked.text = totalPicked.toString()
                //qtyConfirmed.text = totalConfirmed.toString()

                picklistAdapter = PicklistAdapter(items)
                recyclerView.adapter = picklistAdapter
            },
            { err -> Log.d(TAG, err.toString())}
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "token ${Rubian.ACCESS_KEY}"
                return headers
            }
        }
        Rubian.getInstance(baseContext).addToRequestQueue(req)
    }

    fun scan(view: View) {
        try {
            val intent = Intent("com.sunmi.scan")
            intent.setPackage("com.sunmi.sunmiqrcodescanner")
            scanLauncher.launch(intent)
        } catch(e: Exception) {
            val intent = Intent(this, Scanner::class.java)
            scanLauncher.launch(intent)
        }

    }

    var scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data!= null) {
                val bundle = data.extras
                var barcode = bundle!!.getString("barcode")
                Toast.makeText(this, barcode, Toast.LENGTH_LONG).show()
                if (barcode.isNullOrEmpty()) {
                    val result = bundle!!.getSerializable("data") as ArrayList<HashMap<String, String>>
                    val iter = result.iterator()
                    while(iter.hasNext()) {
                        val map = iter.next()
                        barcode = map.get("VALUE")
                    }
                }
                Toast.makeText(this, barcode, Toast.LENGTH_LONG).show()
                val scanIntent = Intent(this, ItemActivity::class.java).apply {
                    for(i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        if (item.getString("barcode").trimStart('0').trim() == barcode!!.trimStart('0').trim()
                            && item.getInt("unconfirmed_picked_qty") == 0) {
                            putExtra("item", item.toString())
                            pickingLauncher.launch(this)
                            break;
                        }
                    }
                }
            }
        }
    }

    private var pickingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        getPicklist(picklist)
    }

    fun finalize(view: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Finalize")
        builder.setMessage("Are you done picking this order?")
        builder.setPositiveButton(R.string.confirm) { dialog, which ->
            val route = "/resource/Pick List/${picklist}"
            var req = object: JsonObjectRequest(
                Request.Method.PUT, Rubian.BASE_URL + route, JSONObject("{'picking_status': 'done'}"), {   it
                },
                { err -> Log.d(TAG, err.toString())}
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "token ${Rubian.ACCESS_KEY}"
                    return headers
                }
            }
            Rubian.getInstance(baseContext).addToRequestQueue(req)

            finish()
        }
        builder.setNegativeButton(R.string.cancel) { dialog, which -> }
        builder.show()
    }

    fun Activity.hideSoftKeyboard(){
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }

}