package com.barapido.rlpicker.uihome

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.barapido.rlpicker.R
import com.squareup.picasso.Picasso
import org.json.JSONArray

class PicklistAdapter(private val array: JSONArray): RecyclerView.Adapter<PicklistAdapter.MyViewHolder> () {
    val picasso = Picasso.get()
    class MyViewHolder(private val listItem: LinearLayout): RecyclerView.ViewHolder(listItem) {
        var image: ImageView = listItem.findViewById(R.id.image)
        var itemName: TextView = listItem.findViewById(R.id.item_name)
        var barcode: TextView = listItem.findViewById(R.id.barcode)
        var location: TextView = listItem.findViewById(R.id.location)
        var uom: TextView = listItem.findViewById(R.id.uom)
        var qty_picked: TextView = listItem.findViewById(R.id.qty_picked)
        var qty_ordered: TextView = listItem.findViewById(R.id.qty_ordered)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val listItem = LayoutInflater.from(parent.context)
            .inflate(R.layout.picklist_item, parent, false) as LinearLayout

        return MyViewHolder(listItem)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val obj = array.getJSONObject(position)
        holder.image.setImageDrawable(ContextCompat.getDrawable(holder.image.context, R.drawable.placeholder_image))
        holder.itemName.text = obj.getString("item_name")
        holder.barcode.text = obj.getString("barcode")
        try {
            holder.location.text = "${obj.getString("warehouse")} (${obj.getString("expiration_date")})"
        } catch (e: Exception) {
            holder.location.text = "${obj.getString("warehouse")}"
        }
        holder.uom.text = obj.getString("uom")
        holder.qty_picked.text = obj.getInt("unconfirmed_picked_qty").toString()
        holder.qty_ordered.text = obj.getInt("qty").toString()
    }

    override fun getItemCount() = array.length()
}