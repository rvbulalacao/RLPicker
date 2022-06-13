package com.barapido.rlpicker.ui.pos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.barapido.rlpicker.R
import com.barapido.rlpicker.api.Rubian
import org.json.JSONArray
import org.json.JSONObject
import java.lang.NumberFormatException

private val TAG = "ItemActivity"

class ItemActivity : AppCompatActivity() {

    private lateinit var item: JSONObject

    private lateinit var itemName: TextView
    private lateinit var barcode: TextView
    private lateinit var location: TextView
    private lateinit var uom: TextView
    private lateinit var qtyPicked: EditText
    private lateinit var qtyOrdered: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pos_item)

        item = JSONObject(intent.getStringExtra("item"))

        itemName = findViewById(R.id.item_name)
        barcode = findViewById(R.id.barcode)
        location = findViewById(R.id.location)
        uom = findViewById(R.id.uom)
        qtyPicked = findViewById(R.id.qty_picked)
        qtyOrdered = findViewById(R.id.qty_ordered)

        displayItem()
    }

    private fun displayItem() {
        itemName.text = item.getString("item_name")
        barcode.text = item.getString("barcode")
        try {
            location.text = "${item.getString("warehouse")} (${item.getString("expiration_date")})"
        } catch(e: Exception) {
            location.text = "${item.getString("warehouse")}"
        }

        uom.text = item.getString("uom")
        qtyPicked.setText(item.getInt("actual_picked_qty").toString())
        qtyOrdered.text = item.getInt("unconfirmed_picked_qty").toString()
    }

    fun confirm(view: View) {
        try {
            val route = "/resource/Pick List Item/${item.getString("name")}"
            val qtyPickedVal = qtyPicked.text.toString().toInt()
            var req = object: JsonObjectRequest(
                Request.Method.PUT, Rubian.BASE_URL + route, JSONObject("{'actual_picked_qty': ${qtyPickedVal}}"), {   it
                    finish()
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
        } catch (e: NumberFormatException) {

        }

    }
}