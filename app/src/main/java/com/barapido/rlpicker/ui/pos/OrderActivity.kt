package com.barapido.rlpicker.ui.pos

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Printer
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.barapido.rlpicker.R
import com.barapido.rlpicker.RLPickerApp
import com.barapido.rlpicker.Scanner
import com.barapido.rlpicker.api.Rubian
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.net.URLEncoder
import java.util.*
import kotlin.collections.HashMap

private const val TAG = "OrderActivity"

private var items = JSONArray()

private lateinit var picklistAdapter: PicklistAdapter

private lateinit var salesOrder: TextView
private lateinit var customer: TextView
private lateinit var requiredDeliverySchedule: TextView
private lateinit var notes: TextView
private lateinit var qtyOrdered: TextView
private lateinit var qtyPicked: TextView
private lateinit var qtyConfirmed: TextView

private lateinit var finalizeBtn: Button
private lateinit var printBtn: Button
private lateinit var scanBtn: Button

private lateinit var recyclerView: RecyclerView

class OrderActivity : AppCompatActivity() {

    companion object {
        private val PERMISSION_BLUETOOTH = 1
    }

    private lateinit var picklist: String
    private lateinit var posBarcode: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)

        picklist = intent.getStringExtra("picklist").toString()

        getPicklist(picklist)

        finalizeBtn = findViewById(R.id.finalize_btn)
        printBtn = findViewById(R.id.print_btn)
        scanBtn = findViewById(R.id.scan_btn)
        posBarcode = findViewById(R.id.pos_barcode)

        if (RLPickerApp.role == "POS") {

            posBarcode.visibility = View.GONE

            /*
            posBarcode.setOnKeyListener(object : View.OnKeyListener {
                override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                    Log.d(TAG, keyCode.toString())
                    if (event!!.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER
                    ) {
                        hideSoftKeyboard()
                        posBarcode.clearFocus()
                        posBarcode.isCursorVisible = false

                        Intent(baseContext, ItemActivity::class.java).apply {
                            var found = false
                            for(i in 0 until items.length()) {
                                val item = items.getJSONObject(i)
                                if (item.getString("barcode").trimStart('0') == posBarcode.text.toString().trimStart('0')
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

                        return true
                    }
                    return false
                }

            });
             */

            posBarcode.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    if (posBarcode.text.toString().isNotEmpty()) {
                        val scanIntent = Intent(baseContext, ItemActivity::class.java).apply {
                            var found = false
                            for(i in 0 until items.length()) {
                                val item = items.getJSONObject(i)
                                if (item.getString("barcode").trimStart('0') == posBarcode.text.toString().trimStart('0')
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
                }

                override fun afterTextChanged(p0: Editable?) { }

            });

        } else {
            posBarcode.visibility = View.GONE
            printBtn.visibility = View.GONE
        }

        salesOrder = findViewById(R.id.sales_order)
        customer = findViewById(R.id.customer)
        requiredDeliverySchedule = findViewById(R.id.required_delivery_date)
        notes = findViewById(R.id.notes)
        qtyOrdered = findViewById(R.id.qty_ordered)
        qtyPicked = findViewById(R.id.qty_picked)
        qtyConfirmed = findViewById(R.id.qty_confirmed)
        qtyConfirmed.visibility = View.VISIBLE


        val picklistManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        picklistAdapter = PicklistAdapter(items)
        recyclerView = findViewById(R.id.picklist)

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
                val docstatus = obj.getInt("docstatus")

                if (RLPickerApp.role == "POS") {
                    scanBtn.visibility = View.INVISIBLE
                    if (docstatus == 0) {
                        finalizeBtn.visibility = View.VISIBLE
                        printBtn.visibility = View.GONE
                        posBarcode.visibility = View.VISIBLE
                    } else {
                        Log.d(TAG, docstatus.toString() +":"+ RLPickerApp.role)
                        finalizeBtn.visibility = View.GONE
                        printBtn.visibility = View.VISIBLE
                        posBarcode.visibility = View.GONE
                    }
                } else {

                    if (docstatus == 0) {
                        if (status == "ongoing") {
                            finalizeBtn.visibility = View.VISIBLE
                            scanBtn.visibility = View.VISIBLE
                        } else {
                            finalizeBtn.visibility = View.GONE
                            scanBtn.visibility = View.INVISIBLE
                        }
                    } else {
                        finalizeBtn.visibility = View.INVISIBLE
                        scanBtn.visibility = View.INVISIBLE
                    }
                    printBtn.visibility = View.GONE
                }

                var totalOrdered = 0
                var totalPicked = 0
                var totalConfirmed = 0
                val locations = obj.getJSONArray("locations");
                for(i in 0 until locations.length()) {
                    val location = locations.getJSONObject(i)
                    totalOrdered += location.getInt("qty")
                    totalPicked += location.getInt("unconfirmed_picked_qty")
                    totalConfirmed += location.getInt("actual_picked_qty")
                    items.put(location)
                }
                qtyOrdered.text = totalOrdered.toString()
                qtyPicked.text = totalPicked.toString()
                qtyConfirmed.text = totalConfirmed.toString()

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
        /*
        val intent = Intent(this, Scanner::class.java)
        scanLauncher.launch(intent)
         */
    }

    var scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data!= null) {
                val bundle = data.extras
                val barcode = bundle!!.getString("barcode")
                val scanIntent = Intent(this, ItemActivity::class.java).apply {
                    for(i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        if (item.getString("barcode").trimStart('0') == barcode!!.trimStart('0')
                            && item.getInt("actual_picked_qty") == 0) {
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
        posBarcode.setText("")
        getPicklist(picklist)
    }

    fun finalize(view: View) {
        if (RLPickerApp.role == "POS") {

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Finalize")
            builder.setMessage("Are you done confirming this order?")
            builder.setPositiveButton(R.string.confirm) { dialog, which ->
                val route = "/resource/Pick List/${picklist}"
                var req = object: JsonObjectRequest(
                    Method.PUT, Rubian.BASE_URL + route, JSONObject("{'docstatus': 1, 'picking_status': 'confirmed'}"), {   it

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
        } else {
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

    }

    fun printReceipt(view: View) {
        val route = "/resource/Sales Order/SAL-ORD-${salesOrder.text}"
        Log.d(TAG, route.toString())
        var req = object: JsonObjectRequest(
            Request.Method.GET, Rubian.BASE_URL + route, null, { res ->

                val route = "/resource/Sales Invoice?fields=[\"name\"]&filters=[[\"against_so\",\"=\",\"SAL-ORD-${salesOrder.text}\"]]"
                var req = object: JsonObjectRequest(
                    Request.Method.GET, Rubian.BASE_URL + route, null, { response ->
                        val sis = response.getJSONArray("data")
                        var si_name = ""
                        if (sis.length() > 0) {
                            si_name = sis.getJSONObject(0).getString("name")
                            val so = res.getJSONObject("data")
                            val so_items = so.getJSONArray("items")

                            val route = "/resource/Sales Invoice/${si_name}"
                            var req = object: JsonObjectRequest(
                                Request.Method.GET, Rubian.BASE_URL + route, null, { response ->
                                    val si = response.getJSONObject("data")
                                    val si_items = si.getJSONArray("items")
                                    var lines = ""
                                    var total_actual_picked_qty = 0
                                    var grand_total = 0.0
                                    for (j in 0 until si_items.length()) {
                                        val si_item = si_items.getJSONObject(j)
                                        total_actual_picked_qty += si_item.getInt("qty")
                                        grand_total += si_item.getInt("qty") * si_item.getDouble("rate")
                                        lines += "[L]${si_item.getString("description")} x${si_item.getInt("qty")}[R]${String.format("%.2f", si_item.getInt("qty") * si_item.getDouble("rate"))}\n"
                                    }
                                    val si_taxes = si.getJSONArray("taxes")
                                    var delivery_fees = 0.0;
                                    for (j in 0 until si_taxes.length()) {
                                        val si_tax = si_taxes.getJSONObject(j)
                                        if (si_tax.getString("description") == "Freight and Forwarding Charges" || si_tax.getString("description") == "Delivery Fee") {
                                            delivery_fees += si_tax.getDouble("tax_amount")
                                        }
                                    }
                                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), OrderActivity.PERMISSION_BLUETOOTH);
                                    } else {
                                        try {
                                            val printer = EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 48f, 32)
                                            printer.printFormattedText(
                                                "[C]${si_name}\n" +
                                                        "[C]${si.getString("against_so")}\n" +
                                                        "[C]${si.getString("store_name")}\n" +
                                                        "[C]${si.getString("shipping_address")
                                                            .replace("<br>", "")
                                                            .replace("\n", ",")
                                                            .replace("Phone: ", "\n")}\n" +
                                                        "[C]${so.getString("delivery_date")}\n" +
                                                        "[C]\n" + lines + "\n" +
                                                        "[C]================================\n" +
                                                        "[R]Delivery Fee: ${String.format("%.2f", delivery_fees)}\n" +
                                                        "[R]${String.format("%.2f", grand_total + delivery_fees)}\n" +
                                                        "[R]Item Count: ${total_actual_picked_qty}\n"
                                            )
                                        } catch(e: Exception) {

                                        }


                                        Log.d(TAG, "[C]${si_name}\n" +
                                                "[C]${si.getString("against_so")}\n" +
                                                "[C]${si.getString("store_name")}\n" +
                                                "[C]${si.getString("shipping_address")
                                                    .replace("<br>", "")
                                                    .replace("\n", ",")
                                                    .replace("Phone: ", "\n")}\n" +
                                                "[C]${so.getString("delivery_date")}\n" +
                                                "[C]\n" + lines + "\n" +
                                                "[C]================================\n" +
                                                "[R]Delivery Fee: ${String.format("%.2f", delivery_fees)}\n" +
                                                "[R]${String.format("%.2f", grand_total + delivery_fees)}\n" +
                                                "[R]Item Count: ${total_actual_picked_qty}\n")
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
                            Rubian.getInstance(baseContext).addToRequestQueue(req)

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
                Rubian.getInstance(baseContext).addToRequestQueue(req)

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

    fun Activity.hideSoftKeyboard(){
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }

}