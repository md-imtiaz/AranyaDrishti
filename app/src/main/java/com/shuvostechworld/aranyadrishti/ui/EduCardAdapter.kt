package com.shuvostechworld.aranyadrishti.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.shuvostechworld.aranyadrishti.R
import com.shuvostechworld.aranyadrishti.data.local.RewardManager
import com.shuvostechworld.aranyadrishti.data.model.EduCard
import java.io.File
import java.io.FileOutputStream

class EduCardAdapter(
    private val onShareIntentReady: (Intent) -> Unit
) : ListAdapter<EduCard, EduCardAdapter.EduCardViewHolder>(EduCardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EduCardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_edu_card, parent, false)
        return EduCardViewHolder(view, onShareIntentReady)
    }

    override fun onBindViewHolder(holder: EduCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EduCardViewHolder(
        itemView: View,
        private val onShareIntentReady: (Intent) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val cardRoot: View = itemView
        private val ivImage: ImageView = itemView.findViewById(R.id.ivEduImage)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvEduTitle)
        private val tvDesc: TextView = itemView.findViewById(R.id.tvEduDesc)
        private val btnShare: ImageButton = itemView.findViewById(R.id.btnShare)

        fun bind(card: EduCard) {
            tvTitle.text = card.title
            tvDesc.text = card.description
            
            ivImage.load(card.imageUrl) {
                crossfade(true)
                placeholder(card.fallbackResId)
                error(card.fallbackResId)
            }

            btnShare.setOnClickListener {
                shareCardAndEarn(cardRoot, card)
            }
        }

        private fun shareCardAndEarn(view: View, card: EduCard) {
            val context = view.context

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val loader = coil.ImageLoader(context)
                    val request = coil.request.ImageRequest.Builder(context)
                        .data(card.imageUrl ?: card.fallbackResId)
                        .allowHardware(false)
                        .build()

                    val result = loader.execute(request)
                    if (result is coil.request.SuccessResult) {
                        val downloadedBitmap = result.drawable.toBitmap()

                        withContext(Dispatchers.Main) {
                            val prevDrawable = ivImage.drawable
                            ivImage.setImageBitmap(downloadedBitmap)

                            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            view.draw(canvas)

                            ivImage.setImageDrawable(prevDrawable)

                            saveAndShareBitmap(context, bitmap)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to load image for sharing.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to share image.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        private fun saveAndShareBitmap(context: android.content.Context, bitmap: Bitmap) {
            try {
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs()
                val imageFile = File(cachePath, "shared_card_${System.currentTimeMillis()}.png")
                val stream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()

                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, "Learn more wildlife facts with Aranya Drishti! Download the app: [PlayStore Link Coming Soon]")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                onShareIntentReady(Intent.createChooser(shareIntent, "Share Info"))
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to share image.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class EduCardDiffCallback : DiffUtil.ItemCallback<EduCard>() {
        override fun areItemsTheSame(oldItem: EduCard, newItem: EduCard): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EduCard, newItem: EduCard): Boolean {
            return oldItem == newItem
        }
    }
}
