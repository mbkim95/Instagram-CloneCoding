package com.project.instagram

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.project.instagram.Navigation.AlarmFragment
import com.project.instagram.Navigation.DetailViewFragment
import com.project.instagram.Navigation.GridFragment
import com.project.instagram.Navigation.UserFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottom_navigation.setOnNavigationItemSelectedListener(this)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            1
        )

        // Set default screen
        bottom_navigation.selectedItemId = R.id.action_home
    }

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        setToolbarDefault()

        when (p0.itemId) {
            R.id.action_home -> {
                val detailViewFragment = DetailViewFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, detailViewFragment)
                    .commit()
                return true
            }

            R.id.action_search -> {
                val gridFragment = GridFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, gridFragment)
                    .commit()
                return true
            }

            R.id.action_add_photo -> {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startActivity(Intent(this, AddPhotoActivity::class.java))
                }
                return true
            }

            R.id.action_favorite_alarm -> {
                val alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, alarmFragment)
                    .commit()
                return true
            }

            R.id.action_account -> {
                val userFragment = UserFragment()
                val bundle = Bundle()
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                bundle.putString("destinationUid", uid)
                userFragment.arguments = bundle
                supportFragmentManager.beginTransaction().replace(R.id.main_content, userFragment)
                    .commit()
                return true
            }
        }
        return false
    }

    fun setToolbarDefault() {
        toolbar_username?.visibility = View.GONE
        toolbar_button_back?.visibility = View.GONE
        toolbar_title_image?.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UserFragment.PICK_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            Log.e("IMAGELOG", "imageUri : ${imageUri}")
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val storageReference =
                FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)
            storageReference.putFile(imageUri!!)
                .continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                    return@continueWithTask storageReference.downloadUrl
                }.addOnSuccessListener { uri ->
                    val map = HashMap<String, Any>()
                    map["image"] = uri.toString()
                    FirebaseFirestore.getInstance().collection("profileImages").document(uid)
                        .set(map)
                }
        }
    }
}
