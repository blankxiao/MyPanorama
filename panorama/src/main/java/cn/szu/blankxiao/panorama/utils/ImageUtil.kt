package cn.szu.blankxiao.panorama.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.facebook.common.executors.CallerThreadExecutor
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.BaseDataSubscriber
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import androidx.core.net.toUri

/**
 * @author BlankXiao
 * @description ImageUtil
 * @date 2025-10-27 0:12
 */
class ImageUtil {


	fun load(target: SimpleDraweeView, uri: Uri?) {
		val requestBuilder = ImageRequestBuilder.newBuilderWithSource(uri)
		configRequestBuilder(requestBuilder)
		load(target, requestBuilder.build())
	}

	fun load(target: SimpleDraweeView, string: String?) {
		load(target, string?.toUri())
	}

	fun load(target: SimpleDraweeView, resId: Int) {
		val requestBuilder: ImageRequestBuilder =
			ImageRequestBuilder.newBuilderWithResourceId(resId)
		configRequestBuilder(requestBuilder)
		load(target, requestBuilder.build())
	}

	private fun load(target: SimpleDraweeView, request: ImageRequest?) {
		val controllerBuilder = Fresco.newDraweeControllerBuilder()
		controllerBuilder.oldController = target.controller
		controllerBuilder.autoPlayAnimations = true
		controllerBuilder.imageRequest = request
		target.controller = controllerBuilder.build()
	}

	companion object {
		fun loadBitmapFromCache(context: Context, url: String): Bitmap? {
			val requestBuilder = ImageRequestBuilder.newBuilderWithSource(url.toUri())
			val request: ImageRequest = requestBuilder.build()
			var bitmap: Bitmap? = null
			val dataSource: DataSource<CloseableReference<CloseableImage>> =
				Fresco.getImagePipeline()
					.fetchImageFromBitmapCache(request, context.applicationContext)
			val ref: CloseableReference<CloseableImage>? = dataSource.getResult()
			try {
				if (ref != null) {
					val result: CloseableImage = ref.get()
					if (result is CloseableBitmap) {
						bitmap = result.underlyingBitmap
					}
				}
			} finally {
				CloseableReference.closeSafely(ref)
				dataSource.close()
			}
			return bitmap
		}

		fun loadBitmapFromNetwork(context: Context, url: String, callback: (Bitmap?) -> Unit) {
			val requestBuilder: ImageRequestBuilder =
				ImageRequestBuilder.newBuilderWithSource(url.toUri())
			configRequestBuilder(requestBuilder)

			val dataSource: DataSource<CloseableReference<CloseableImage>> =
				Fresco.getImagePipeline()
					.fetchDecodedImage(requestBuilder.build(), context.applicationContext)
			dataSource.subscribe(object : BaseDataSubscriber<CloseableReference<CloseableImage>>() {
				public override fun onNewResultImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
					if (!dataSource.isFinished) {
						return
					}
					val ref: CloseableReference<CloseableImage>? = dataSource.getResult()
					if (ref != null) {
						val result: CloseableImage = ref.get()
						val bitmap: Bitmap = (result as CloseableBitmap).underlyingBitmap
						callback(bitmap)
					}
				}

				public override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
					val t: Throwable? = dataSource.failureCause
					if (t != null) {
						t.printStackTrace()
						callback(null)
					}
				}
			}, CallerThreadExecutor.getInstance())
		}

		fun init(applicationContext: Context?) {
			Fresco.initialize(applicationContext)
		}

		private fun configRequestBuilder(requestBuilder: ImageRequestBuilder) {
			requestBuilder.rotationOptions = RotationOptions.autoRotate()
			requestBuilder.lowestPermittedRequestLevel = ImageRequest.RequestLevel.FULL_FETCH
			requestBuilder.isProgressiveRenderingEnabled = false
		}
	}


}