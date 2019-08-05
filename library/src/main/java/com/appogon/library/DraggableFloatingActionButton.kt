package com.appogon.library

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.sqrt

/**
 * A custom floating action button that support dragging.
 *
 * @constructor Creates a DraggableFloatingActionButton.
 */
class DraggableFloatingActionButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    val isDraggingLeft = BehaviorSubject.createDefault(false)
    val isDraggingRight = BehaviorSubject.createDefault(false)
    val isDraggingTop = BehaviorSubject.createDefault(false)
    val isDraggingTopRight = BehaviorSubject.createDefault(false)
    val isDraggingTopLeft = BehaviorSubject.createDefault(false)
    val isDraggingBottom = BehaviorSubject.createDefault(false)
    val isDraggingBottomRight = BehaviorSubject.createDefault(false)
    val isDraggingBottomLeft = BehaviorSubject.createDefault(false)

    private val isNowDragging = BehaviorSubject.create<Boolean>()

    private var downRawX: Float = 0f
    private var downRawY: Float = 0f

    private var dX: Float = 0f
    private var dY: Float = 0f

    private var cX: Float = 0f
    private var cY: Float = 0f

    private var radius: Float = 0f

    private var lastAngle = 0
    private val lastPower = 0

    private lateinit var disposable: CompositeDisposable

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        disposable = CompositeDisposable()
        subscribeToDraggingState()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposable.dispose()
    }

    init {
        setOnTouchListener { view: View, motionEvent: MotionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> return@setOnTouchListener onActionDown(view, motionEvent)
                MotionEvent.ACTION_MOVE -> return@setOnTouchListener onActionMove(view, motionEvent)
                MotionEvent.ACTION_UP -> return@setOnTouchListener onActionUp(view, motionEvent)
                else -> return@setOnTouchListener super.onTouchEvent(motionEvent)
            }
        }
    }

    /**
     * A function that handles event that was dispatched to button
     * when a pressing gesture received
     *
     * @property view The current button
     * @property motionEvent The event
     * @return True if the event was consumed, false otherwise
     */
    private fun onActionDown(view: View, motionEvent: MotionEvent): Boolean {
        cX = view.x
        cY = view.y

        radius = height * DRAG_RADIUS_WITH_RESPECT_TO_BUTTON_SIZE

        downRawX = motionEvent.rawX
        downRawY = motionEvent.rawY

        dX = view.x - downRawX
        dY = view.y - downRawY

        return true
    }

    /**
     * A function that handles event that was dispatched to button
     * when a drag has happened during a pressing gesture
     *
     * @property view The current button
     * @property motionEvent The event
     * @return True if the event was consumed, false otherwise
     */
    private fun onActionMove(view: View, motionEvent: MotionEvent): Boolean {
        if (isNowDragging.value != true && !isClick(motionEvent)) {
            isNowDragging.onNext(true)
        }

        var newX = motionEvent.rawX + dX
        var newY = motionEvent.rawY + dY

        val abs = sqrt((newX - cX) * (newX - cX) + (newY - cY) * (newY - cY))
        if (abs > radius) {
            newX = (newX - cX) * radius / abs + cX
            newY = (newY - cY) * radius / abs + cY
        }

        if (getPowerOfButtonMovement(newX, newY) >= POWER_BUTTON_MOVEMENT_TO_RESPOND) {

            when (getMovementDirection(newX, newY)) {
                TOP -> {
                    if (isDraggingTop.value != true) {
                        isDraggingTop.onNext(true)
                        isDraggingBottom.onNext(false)
                    }
                }
                TOP_RIGHT -> {
                    if (isDraggingTopRight.value != true) {
                        isDraggingTopRight.onNext(true)
                    }
                }
                TOP_LEFT -> {
                    if (isDraggingTopLeft.value != true) {
                        isDraggingTopLeft.onNext(true)
                    }
                }
                RIGHT -> {
                    if (isDraggingRight.value != true) {
                        isDraggingRight.onNext(true)
                    }
                }
                BOTTOM -> {
                    if (isDraggingBottom.value != true) {
                        isDraggingBottom.onNext(true)
                        isDraggingTop.onNext(false)
                    }
                }
                BOTTOM_RIGHT -> {
                    if (isDraggingBottomRight.value != true) {
                        isDraggingBottomRight.onNext(true)
                    }
                }
                BOTTOM_LEFT -> {
                    if (isDraggingBottomLeft.value != true) {
                        isDraggingBottomLeft.onNext(true)
                    }
                }
                LEFT -> {
                    if (isDraggingLeft.value != true) {
                        isDraggingLeft.onNext(true)
                    }
                }
            }
        }

        view.animate().x(newX).y(newY).setDuration(0).start()
        return true
    }

    /**
     * A function that handles event that was dispatched to button
     * when a pressing gesture has finished
     *
     * @property view The current button
     * @property motionEvent The event
     * @return True if the event was consumed, false otherwise
     */
    private fun onActionUp(view: View, motionEvent: MotionEvent): Boolean {
        view.animate().x(cX).y(cY).setDuration(0).start()
        resetDragDirectionsStates()
        return if (isClick(motionEvent)) {
            performClick()
        } else {
            true
        }
    }

    /**
     * Subscribing to current dragging state
     */
    private fun subscribeToDraggingState() {
        var scale: Float
        disposable.add(isNowDragging.subscribe {
            scale = if (it) {
                BUTTON_BIG_SCALE_MULTIPLIER
            } else {
                BUTTON_REGULAR_SCALE_MULTIPLIER
            }
            scaleX = scale
            scaleY = scale
        })
    }

    /**
     * A function that makes a check on whether the click was made or the movement started
     *
     * @property motionEvent The event
     * @return True if click consumed
     */
    private fun isClick(motionEvent: MotionEvent): Boolean {

        val upDX = motionEvent.rawX - downRawX
        val upDY = motionEvent.rawY - downRawY

        return abs(upDX) < CLICK_DRAG_TOLERANCE && abs(upDY) < CLICK_DRAG_TOLERANCE
    }

    /**
     * A function that returns current angle of the button movement
     *
     * @property x The new position x
     * @property y The new position y
     * @return The numerical value of the angle of the button movement
     */
    private fun getMovementAngle(x: Float, y: Float): Int {
        when {
            x > cX -> lastAngle = when {
                y < cY -> (atan((y - cY) / (x - cX)) * RAD + 90).toInt()
                y > cY -> (atan((y - cY) / (x - cX)) * RAD).toInt() + 90
                else -> 90
            }
            x < cX -> lastAngle = when {
                y < cY -> (atan((y - cY) / (x - cX)) * RAD - 90).toInt()
                y > cY -> (atan((y - cY) / (x - cX)) * RAD).toInt() - 90
                else -> -90
            }
            else -> lastAngle = if (y <= cY) {
                0
            } else {
                if (lastAngle < 0) {
                    -180
                } else {
                    180
                }
            }
        }
        return lastAngle
    }

    /**
     * A function that returns current power of the button movement
     *
     * @property x The new position x
     * @property y The new position y
     * @return The numerical value of the power of the button movement from 0 to 100
     */
    private fun getPowerOfButtonMovement(x: Float, y: Float): Int {
        return (100 * sqrt((x - cX) * (x - cX) + (y - cY) * (y - cY)) / radius).toInt()
    }

    /**
     * A function that returns current direction of the button movement
     *
     * @property x The new position x
     * @property y The new position y
     * @return The numerical value of the direction of the button movement
     */
    private fun getMovementDirection(x: Float, y: Float): Int {
        getMovementAngle(x, y)
        if (lastPower == 0 && lastAngle == 0) {
            return 0
        }
        var a = 0
        if (lastAngle <= 0) {
            a = lastAngle * -1 + 90
        } else if (lastAngle > 0) {
            a = if (lastAngle <= 90) {
                90 - lastAngle
            } else {
                360 - (lastAngle - 90)
            }
        }
        var direction = (a + 22) / 45 + 1
        if (direction > 8) {
            direction = 1
        }
        return direction
    }

    /**
     * A function that reset Behavior Subjects that respond for directions states to default value
     */
    private fun resetDragDirectionsStates() {
        isNowDragging.onNext(false)
        isDraggingLeft.onNext(false)
        isDraggingRight.onNext(false)
        isDraggingTop.onNext(false)
        isDraggingTopLeft.onNext(false)
        isDraggingTopRight.onNext(false)
        isDraggingBottom.onNext(false)
        isDraggingBottomLeft.onNext(false)
        isDraggingBottomRight.onNext(false)
    }

    companion object {

        // Fundamental
        private const val RAD = 57.2957795

        // Button settings
        private const val CLICK_DRAG_TOLERANCE = 10f // to prevent slight drags when user taps fab
        private const val DRAG_RADIUS_WITH_RESPECT_TO_BUTTON_SIZE = 0.45f
        private const val BUTTON_BIG_SCALE_MULTIPLIER = 1.1f
        private const val BUTTON_REGULAR_SCALE_MULTIPLIER = 1f
        private const val POWER_BUTTON_MOVEMENT_TO_RESPOND = 50

        // Button directions
        private const val TOP = 3
        private const val TOP_LEFT = 4
        private const val RIGHT = 1
        private const val TOP_RIGHT = 2
        private const val BOTTOM = 7
        private const val BOTTOM_RIGHT = 8
        private const val LEFT = 5
        private const val BOTTOM_LEFT = 6
    }

}