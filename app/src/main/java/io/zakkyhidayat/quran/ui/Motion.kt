package io.zakkyhidayat.quran.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Spesifikasi gerak tunggal (Material shared axis X) untuk onboarding dan navigasi antarlayar, agar terasa sama.
const val STEP_MS = 300
const val FADE_OUT_MS = 90
const val FADE_IN_MS = STEP_MS - FADE_OUT_MS
val StepEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f) // emphasized decelerate
val StepSlide = 30.dp

/**
 * Masuk: geser dari [slidePx] pada sisi "maju" + pudar masuk tertunda [FADE_OUT_MS] (tanpa tumpang tindih dengan
 * layar yang keluar). [forward] = 1 untuk maju, -1 untuk mundur (sudah dikalikan arah RTL oleh pemanggil).
 */
fun sharedAxisXEnter(forward: Int, slidePx: Int, delayedFade: Boolean = true): EnterTransition =
    slideInHorizontally(motionTween(STEP_MS, easing = StepEasing)) { forward * slidePx } +
        fadeIn(motionTween(if (delayedFade) FADE_IN_MS else STEP_MS, delayMillis = if (delayedFade) FADE_OUT_MS else 0, easing = LinearEasing))

/** Keluar: geser berlawanan arah + pudar cepat pada 90 ms pertama. */
fun sharedAxisXExit(forward: Int, slidePx: Int): ExitTransition =
    slideOutHorizontally(motionTween(STEP_MS, easing = StepEasing)) { -forward * slidePx } +
        fadeOut(motionTween(FADE_OUT_MS, easing = LinearEasing))

/** Pergeseran kecil layar yang keluar ke tepi gestur dan radius sudut (extra large) selama kembali prediktif. */
val PredictiveShift = 16.dp
val PredictiveCorner = 28.dp

/**
 * Kembali prediktif (gestur back, pedoman MD3): transisi ini dipakai juga untuk seek mengikuti jari, jadi semua animasi
 * berjalan sepanjang durasi penuh tanpa delay. Layar yang keluar mengecil ke 0.9, hanya bergeser sedikit
 * ([distancePx]) ke tepi gestur, dan memudar; sudut membulat lewat [PredictiveBackFrame]. Saat jari dilepas (commit)
 * NavHost melanjutkan animasi ini sampai selesai dengan easing emphasized yang sama seperti pop biasa; saat batal
 * animasinya berbalik.
 */
fun predictivePopExit(forward: Int, distancePx: Int): ExitTransition =
    slideOutHorizontally(motionTween(STEP_MS, easing = StepEasing)) { forward * distancePx } +
        scaleOut(motionTween(STEP_MS, easing = StepEasing), targetScale = 0.9f) +
        fadeOut(motionTween(STEP_MS, easing = LinearEasing))

/** Layar di bawah: parallax halus dari -[slidePx] searah + pudar masuk sepanjang durasi, tanpa delay. */
fun predictivePopEnter(forward: Int, slidePx: Int): EnterTransition =
    slideInHorizontally(motionTween(STEP_MS, easing = StepEasing)) { -forward * slidePx } +
        fadeIn(motionTween(STEP_MS, easing = LinearEasing))

/**
 * Membulatkan sudut layar yang sedang keluar (PostExit) dan memotongnya, sehingga kartu mengecil saat back prediktif
 * tampak berujung bulat. Radius dianimasikan mengikuti transisi NavHost (ikut seek gestur); layar yang diam tanpa clip.
 */
@Composable
fun AnimatedContentScope.PredictiveBackFrame(content: @Composable () -> Unit) {
    val radius by transition.animateDp(transitionSpec = { motionTween(STEP_MS, easing = LinearEasing) }, label = "backCorner") {
        if (it == EnterExitState.PostExit) PredictiveCorner else 0.dp
    }
    Box(
        Modifier.graphicsLayer {
            if (radius > 0.dp) {
                shape = RoundedCornerShape(radius)
                clip = true
            }
        },
    ) { content() }
}

/**
 * Pengurangan gerak: aktif bila sistem mematikan animasi (skala animator 0, opsi "Remove animations" atau setara).
 * Dibaca dari [QuranTheme] setiap komposisi; spesifikasi di bawah ini memakai [ReduceMotion.enabled] saat animasi dimulai.
 */
object ReduceMotion {
    @Volatile
    var enabled: Boolean = false

    fun systemAnimationsOff(context: android.content.Context): Boolean =
        android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}

/** Sama seperti [androidx.compose.animation.core.tween], tetapi langsung lompat bila pengurangan gerak aktif. */
fun <T> motionTween(durationMillis: Int, delayMillis: Int = 0, easing: Easing = androidx.compose.animation.core.FastOutSlowInEasing): FiniteAnimationSpec<T> =
    if (ReduceMotion.enabled) snap(delayMillis) else androidx.compose.animation.core.tween(durationMillis, delayMillis, easing)

/** Spesifikasi dari MotionScheme M3 ikut pengurangan gerak. */
fun <T> reducedMotion(spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> = if (ReduceMotion.enabled) snap() else spec
