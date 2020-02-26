package com.project.instagram.Navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.instagram.Navigation.model.ContentDTO
import com.project.instagram.R
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment() {
    var firestore: FirebaseFirestore? = null
    var uid: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

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

            // Like button event handling
            viewHolder.detailView_item_favorite_imageView.setOnClickListener {
                favoriteEvent(position)
            }

            // This code is when the page is loaded
            if (contentDTOs[position].favorites.containsKey(uid)) {
                viewHolder.detailView_item_favorite_imageView.setImageResource(R.drawable.ic_favorite)
            } else {
                viewHolder.detailView_item_favorite_imageView.setImageResource(R.drawable.ic_favorite_border)
            }
        }

        fun favoriteEvent(position: Int) {
            val tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction ->
                uid = FirebaseAuth.getInstance().currentUser?.uid
                val contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if (contentDTO!!.favorites.containsKey(uid)) {
                    // When the favorite button is clicked
                    contentDTO.favoriteCount -= 1
                    contentDTO.favorites.remove(uid)
                } else {
                    // When the favorite button is not clicked
                    contentDTO.favoriteCount += 1
                    contentDTO.favorites[uid!!] = true
                }
                transaction.set(tsDoc, contentDTO)
            }
        }


    }

    inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
