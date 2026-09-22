package com.saber.myapp

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProcessedImagesAdapter(
    private var images: List<File>
) : RecyclerView.Adapter<ProcessedImagesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val image: ImageView =
            view.findViewById(R.id.imgProcessed)

        val type: TextView =
            view.findViewById(R.id.tvProcessedType)

        val date: TextView =
            view.findViewById(R.id.tvProcessedDate)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_processed_image,
                parent,
                false
            )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val file = images[position]

        val bitmap =
            BitmapFactory.decodeFile(file.absolutePath)

        holder.image.setImageBitmap(bitmap)

        val type =
            if (file.name.startsWith("opencv_")) {
                "OpenCV"
            } else {
                "المعالجة العادية"
            }

        holder.type.text = type

        val dateFormat =
            SimpleDateFormat(
                "yyyy/MM/dd - HH:mm:ss",
                Locale.getDefault()
            )

        holder.date.text =
            dateFormat.format(
                Date(file.lastModified())
            )
    }

    override fun getItemCount(): Int {
        return images.size
    }

    fun updateImages(newImages: List<File>) {
        images = newImages
        notifyDataSetChanged()
    }
}
