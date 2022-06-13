package com.barapido.rlpicker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase

private val TAG = "SignInActivity"

class SignInActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var uname: EditText
    private lateinit var pword: EditText
    private lateinit var roles: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        auth = FirebaseAuth.getInstance()

        uname = findViewById(R.id.username)
        pword = findViewById(R.id.password)

        roles = findViewById(R.id.roles)
        ArrayAdapter.createFromResource(this,
            R.array.roles_array,
            android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            roles.adapter = adapter
        }
    }

    fun signIn(view: View) {
        Log.d(TAG, uname.text.toString() + ":" + pword.text.toString())
        auth.signInWithEmailAndPassword(uname.text.toString(), pword.text.toString())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    RLPickerApp.role = roles.selectedItem.toString()
                    if (RLPickerApp.role == "POS") {
                        val intent = Intent(this, POSActivity::class.java)
                        startActivity(intent)
                    } else {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    }

                } else {
                    Toast.makeText(this, "Failed to authenticate", Toast.LENGTH_SHORT).show()
                }
            }

    }
}