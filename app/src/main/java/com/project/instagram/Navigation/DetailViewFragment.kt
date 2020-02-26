package com.project.instagram.Navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.project.instagram.Navigation.model.ContentDTO
import com.project.instagram.R
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment() {
    var firestore: FirebaseFirestore? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)
        firestore = FirebaseFirestore.getInstance()

        view.detailViewFragment_recyclerView.adapter = DetailViewRecyclerViewAdapter()
        view.detailViewFragment_recyclerView.layoutManager = LinearLayoutManager(activity)
        return view
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        var contentUidList: ArrayList<String> = arrayListOf()

        init {
            firestore?.collection("images")?.orderBy("timestamp")
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()
                    contentUidList.clear()

                    for (snapshot in querySnapshot!!.documents) {
                        val item = snapshot.toObject(ContentDTO::class.java)
                        contentDTOs.add(item!!)
                        contentUidList.add(snapshot.id)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
            return CustomViewHolder(view)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewHolder = (holder as CustomViewHolder).itemView

            // UserId
            viewHolder.detailView_item_profile_textView.text = contentDTOs[position].userId

            // Image
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                .into(viewHolder.detailView_item_imageView_content)

            // Explain of content
            viewHolder.detailView_item_explain_textView.text = contentDTOs[position].explain

            // Like count
            viewHolder.detailView_item_favorite_count_textView.text =
                "Likes ${contentDTOs[position].favoriteCount}"

            // Profile Image
            Glide.with(viewHolder.context).load(contentDTOs[position].imageUrl)
                .into(viewHolder.detailView_item_profile_image)
        }

    }

    inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
