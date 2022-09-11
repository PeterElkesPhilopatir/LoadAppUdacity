package com.peter.loadappudacity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.peter.loadappudacity.databinding.ActivityDetailsBinding

class DetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_details);

        if (intent?.extras != null) {
            binding.fileNameTextView.text = intent.getStringExtra("fileName")
            binding.statusTextView.text = intent.getStringExtra("status")
        }

        binding.okButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}