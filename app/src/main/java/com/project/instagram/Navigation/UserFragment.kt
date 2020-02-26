package com.project.instagram.Navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.instagram.LoginActivity
import com.project.instagram.MainActivity
import com.project.instagram.Navigation.model.ContentDTO
import com.project.instagram.Navigation.model.FollowDTO
import com.project.instagram.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment() {
    companion object {
        const val PICK_PROFILE_FROM_ALBUM = 10
    }

    var fragmentView: View? = null
    var firestore: FirebaseFirestore? = null
    var uid: String? = null
    var auth: FirebaseAuth? = null
    var currentUid: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView =
            LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUid = auth?.currentUser?.uid

        if (uid == currentUid) {
            // My page
            fragmentView?.account_button_follow_singout?.text = getString(R.string.signout)
            fragmentView?.account_button_follow_singout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        } else {
            // Other user page
            account_button_follow_singout?.text = getString(R.string.follow)
            val mainActivity = (activity as MainActivity)
            mainActivity.toolbar_username?.text = arguments?.getString("userId")
            mainActivity.toolbar_button_back.setOnClickListener {
                mainActivity.bottom_navigation.selectedItemId = R.id.action_home
            }
            mainActivity.toolbar_title_image?.visibility = View.GONE
            mainActivity.toolbar_username?.visibility = View.VISIBLE
            mainActivity.toolbar_button_back?.visibility = View.VISIBLE
            fragmentView?.account_button_follow_singout?.setOnClickListener {
                requestFollow()
            }
        }

        fragmentView?.account_recyclerView?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerView?.layoutManager = GridLayoutManager(activity!!, 3)

        fragmentView?.account_imageView_profile?.setOnClickListener {
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }
        getProfileImage()
        getFollowerAndFollowing()
        return fragmentView
    }

    fun getProfileImage() {
        firestore?.collection("profileImages")?.document(uid!!)
            ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (documentSnapshot == null) {
                    return@addSnapshotListener
                }
                if (documentSnapshot.data != null) {
                    var data = documentSnapshot.data
                    val url = documentSnapshot.data!!["image"]
                    Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop())
                        .into(fragmentView?.account_imageView_profile!!)
                }
            }
    }

    fun getFollowerAndFollowing() {
        firestore?.collection("users")?.document(uid!!)
            ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (documentSnapshot == null) {
                    return@addSnapshotListener
                }
                var followDTO = documentSnapshot.toObject(FollowDTO::class.java)

                if (followDTO?.followingCount != null) {
                    fragmentView?.account_textView_following_count?.text =
                        followDTO.followingCount.toString()
                }
                if (followDTO?.followingCount != null) {
                    fragmentView?.account_textView_follower_count?.text =
                        followDTO.followerCount.toString()
                    if (followDTO.followers.containsKey(currentUid!!)) {
                        fragmentView?.account_button_follow_singout?.text =
                            getString(R.string.follow_cancel)
                        fragmentView?.account_button_follow_singout?.background?.setColorFilter(
                            ContextCompat.getColor(activity!!, R.color.colorLightGray),
                            PorterDuff.Mode.MULTIPLY
                        )
                    } else {
                        if (uid != currentUid) {
                            fragmentView?.account_button_follow_singout?.text =
                                getString(R.string.follow)
                            fragmentView?.account_button_follow_singout?.background?.colorFilter =
                                null
                        }
                    }
                }
            }
    }

    fun requestFollow() {
        // Save data to my account
        val tsDocFollowing = firestore?.collection("users")?.document(currentUid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followings[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            if (followDTO.followings.containsKey(uid)) {
                // It remove following third person when a third person follow me
                followDTO.followingCount -= 1
                followDTO.followings.remove(uid)
            } else {
                // It add following third person when a third person don't follow me
                followDTO.followingCount += 1
                followDTO.followings.put(uid!!, true)
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }
        // Save data to third person
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUid!!] = true

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }
            if (followDTO!!.followers.containsKey(currentUid)) {
                // It cancel my follower when I follow a third person
                followDTO!!.followerCount -= 1
                followDTO!!.followers.remove(currentUid)
            } else {
                // It add my follower when I follow a third person
                followDTO!!.followerCount += 1
                followDTO!!.followers[currentUid!!] = true
            }
            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }


    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")?.whereEqualTo("uid", uid)
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    // Sometimes, This code return null of querySnapshot when it signout
                    if (querySnapshot == null) {
                        return@addSnapshotListener
                    }

                    // Get data
                    for (snapshot in querySnapshot.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }
                    fragmentView?.account_textView_post_count?.text = contentDTOs.size.toString()
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val width = resources.displayMetrics.widthPixels / 3
            val imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageView)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val imageView = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop()).into(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) :
            RecyclerView.ViewHolder(imageView) {
        }
    }
}