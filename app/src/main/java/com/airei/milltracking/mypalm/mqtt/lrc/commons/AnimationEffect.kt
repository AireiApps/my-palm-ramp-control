package com.airei.milltracking.mypalm.mqtt.lrc.commons

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.BounceInterpolator
import android.widget.ImageView

fun fadeInByObject(imageView: ImageView, duration: Long = 2600) {
    val fadeIn = ObjectAnimator.ofFloat(imageView, "alpha", 0f, 1f)
    fadeIn.duration = duration
    fadeIn.start()
}

// Fade out
fun fadeOutByObject(imageView: ImageView, duration: Long = 2000) {
    val fadeOut = ObjectAnimator.ofFloat(imageView, "alpha", 1f, 0f)
    fadeOut.duration = duration
    fadeOut.start()
}

// Fade in
fun fadeInByAlpha(imageView: ImageView, duration: Long = 2000) {
    val fadeIn = AlphaAnimation(0f, 1f)
    fadeIn.duration = duration
    imageView.startAnimation(fadeIn)
    imageView.visibility = View.VISIBLE
}

// Fade out
fun fadeOutByAlpha(imageView: ImageView, duration: Long = 2000) {
    val fadeOut = AlphaAnimation(1f, 0f)
    fadeOut.duration = duration
    imageView.startAnimation(fadeOut)
    imageView.visibility = View.INVISIBLE
}

fun applyBounceAnimation(view: View) {
    // Create scaling animations for X and Y axes
    val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.7f, 1.0f)
    val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.7f, 1.0f)

    // Apply a bounce interpolator to both scaling animations
    scaleX.interpolator = BounceInterpolator()
    scaleY.interpolator = BounceInterpolator()

    // Set duration for the animations
    scaleX.duration = 800
    scaleY.duration = 800

    // Combine both animations and start them together
    val animatorSet = AnimatorSet()
    animatorSet.playTogether(scaleX, scaleY)
    animatorSet.start()
}

fun applyDismissAnimation(view: View?, onAnimationEnd: () -> Unit) {
    if (view == null) return
    // Create reverse scaling animations for X and Y axes (shrink effect)
    val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.7f)
    val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 0.7f)
    // Create a fade-out animation (alpha from 1 to 0)
    val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1.0f, 0.0f)
    // Set duration and interpolators for all animations
    scaleX.duration = 500
    scaleY.duration = 500
    fadeOut.duration = 500
    // Combine the animations
    val animatorSet = AnimatorSet()
    animatorSet.playTogether(scaleX, scaleY, fadeOut)
    // Add a listener to dismiss the dialog when the animation ends
    animatorSet.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            super.onAnimationEnd(animation)
            onAnimationEnd() // Dismiss the dialog
        }
    })
    // Start the animation
    animatorSet.start()
}

fun applyZoomAndFadeAnimation(view: View?, duration: Long = 500) {
    if (view == null) return

    // Create zoom in animations for X and Y axes (scaling up)
    val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.7f, 1.0f)
    val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.7f, 1.0f)

    // Create fade-in animation (alpha from 0 to 1)
    val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0.0f, 1.0f)

    // Set duration and interpolators
    scaleX.duration = duration
    scaleY.duration = duration
    fadeIn.duration = duration

    // Combine the animations into an AnimatorSet
    val animatorSet = AnimatorSet()
    animatorSet.playTogether(scaleX, scaleY, fadeIn)

    // Start the animation
    animatorSet.start()
}