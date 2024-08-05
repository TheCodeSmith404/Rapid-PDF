package com.tcs.tools.managePdf.ui.onBoarding

import android.content.Context
import android.graphics.BitmapFactory
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.tcs.tools.managePdf.R
import com.tcs.tools.managePdf.databinding.OnBoardingItemBinding

class OnBoardingAdapter(private val context:Context):RecyclerView.Adapter<OnBoardingAdapter.ViewHolder>(){
    private val titles=context.resources.getStringArray(R.array.on_boarding_title)
    private val description=context.resources.getStringArray(R.array.on_boarding_description)
    inner class ViewHolder(item:OnBoardingItemBinding ):RecyclerView.ViewHolder(item.root){
        val imageView=item.image
        val title=item.titleOnBoarding
        val description=item.descriptionOnBoarding
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding=OnBoardingItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return 5
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.description.text= HtmlCompat.fromHtml(description[position], HtmlCompat.FROM_HTML_MODE_LEGACY)
        holder.title.text=titles[position]
        val id=when(position){
            1->R.raw.on_boarding_1
            2->R.raw.on_boarding_2
            3->R.raw.on_boarding_3
            4->R.raw.on_boarding_4
            else -> { R.raw.on_boarding_0}
        }
        val inputStream = context.resources.openRawResource(id)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        holder.imageView.setImageBitmap(bitmap)
    }
}