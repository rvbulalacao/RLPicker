package com.barapido.rlpicker.ui.pos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsSeekBar
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.barapido.rlpicker.R
import org.json.JSONArray

class OrdersListAdapter(private val array: JSONArray, private val onItemClicked:(position: Int) -> Unit): RecyclerView.Adapter<OrdersListAdapter.MyViewHolder> () {

    final val LOADING = 0
    final val ITEM = 1

    class MyViewHolder(private val listItem: LinearLayout, private val onItemClicked: (position: Int) -> Unit): RecyclerView.ViewHolder(listItem), View.OnClickListener {
        var salesOrder: TextView = listItem.findViewById(R.id.sales_order)
        var customer: TextView = listItem.findViewById(R.id.customer)
        var requiredDeliveryDate: TextView = listItem.findViewById(R.id.required_delivery_date)
        var addressCity: TextView = listItem.findViewById(R.id.address_city)
        init {
            itemView.setOnClickListener(this)
        }
        override fun onClick(p0: View?) {
            val position = adapterPosition
            onItemClicked(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val listItem = LayoutInflater.from(parent.context)
            .inflate(R.layout.pending_orders_list_item, parent, false) as LinearLayout
        return MyViewHolder(listItem, onItemClicked)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val obj = array.getJSONObject(position)
        holder.salesOrder.text = obj.getString("sales_order")
        holder.customer.text = obj.getString("store_name")
        holder.requiredDeliveryDate.text = obj.getString("required_delivery_date")
        try {
            holder.addressCity.text = obj.getString("address_city")
        }catch (e: Exception) {}

        /*
        when(getItemViewType(position)) {
            ITEM -> {
                holder.salesOrder.text = obj.getString("sales_order")
                holder.customer.text = obj.getString("store_name")
                holder.requiredDeliveryDate.text = obj.getString("required_delivery_date")
                try {
                    holder.addressCity.text = obj.getString("address_city")
                }catch (e: Exception) {}
            }
            LOADING:
                loadingViewHolder = (LoadingViewHolder) holder
        }
        */

    }



    override fun getItemCount() = array.length()

    class LoadingViewHolder: RecyclerView.ViewHolder {
        private var progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)

        constructor(itemView: View) : super(itemView)
    }
}