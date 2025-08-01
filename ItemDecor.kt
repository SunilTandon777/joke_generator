/**
 * Created by Mark O'Sullivan on 25th February 2018.
 */
@SuppressLint("RtlHardcoded")
class ItemDecor : ConstraintLayout {
    /**
     * Main view is the view which is shown when the layout is closed.
     */
    private var mMainView: View? = null

    /**
     * Secondary view is the view which is shown when the layout is opened.
     */
    private var mSecondaryView: View? = null

    /**
     * The rectangle position of the main view when the layout is closed.
     */
    private val mRectMainClose = Rect()

    /**
     * The rectangle position of the main view when the layout is opened.
     */
    private val mRectMainOpen = Rect()

    /**
     * The rectangle position of the secondary view when the layout is closed.
     */
    private val mRectSecClose = Rect()

    /**
     * The rectangle position of the secondary view when the layout is opened.
     */
    private val mRectSecOpen = Rect()

    /**
     * The minimum distance (px) to the closest drag edge that the SwipeRevealLayout
     * will disallow the parent to intercept touch event.
     */
    private var mMinDistRequestDisallowParent = 0
    private var mIsOpenBeforeInit = false

    @Volatile
    private var mIsScrolling = false

    /**
     * @return true if the drag/swipe motion is currently locked.
     */
    @Volatile
    var isDragLocked = false
    private var mMinFlingVelocity = DEFAULT_MIN_FLING_VELOCITY
    private var mMode = MODE_NORMAL
    private var mDragEdge = DRAG_EDGE_LEFT
    private var mDragDist = 0f
    private var mPrevX = -1f
    private var mDragHelper: ViewDragHelper? = null
    private var mGestureDetector: GestureDetectorCompat? = null

    constructor(context: Context?) : super(context!!) {
        init(context, null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable(SUPER_INSTANCE_STATE, super.onSaveInstanceState())
        return super.onSaveInstanceState()
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        var state: Parcelable? = state
        val bundle = state as Bundle?
        state = bundle!!.getParcelable(SUPER_INSTANCE_STATE)
        super.onRestoreInstanceState(state)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mGestureDetector!!.onTouchEvent(event)
        mDragHelper!!.processTouchEvent(event)
        return true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isDragLocked) {
            return super.onInterceptTouchEvent(ev)
        }
        mDragHelper!!.processTouchEvent(ev)
        mGestureDetector!!.onTouchEvent(ev)
        accumulateDragDist(ev)
        val couldBecomeClick = couldBecomeClick(ev)
        val settling = mDragHelper!!.viewDragState == ViewDragHelper.STATE_SETTLING
        val idleAfterScrolled = (mDragHelper!!.viewDragState == ViewDragHelper.STATE_IDLE
                && mIsScrolling)

        // must be placed as the last statement
        mPrevX = ev.x

        // return true => intercept, cannot trigger onClick event
        return !couldBecomeClick && (settling || idleAfterScrolled)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        // get views
        if (childCount >= 2) {
            mSecondaryView = getChildAt(0)
            mMainView = getChildAt(1)
        } else if (childCount == 1) {
            mMainView = getChildAt(0)
        }
    }

    /**
     * Override onLayout to handle swipe reveal positioning while letting ConstraintLayout
     * handle the initial constraint-based positioning
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // Let ConstraintLayout do its normal layout first
        super.onLayout(changed, left, top, right, bottom)
        
        // Store the constraint-based positions as our base positions
        initRects()
        
        // Apply any offset for SAME_LEVEL mode
        if (mMode == MODE_SAME_LEVEL && mSecondaryView != null) {
            when (mDragEdge) {
                DRAG_EDGE_LEFT -> mSecondaryView!!.offsetLeftAndRight(mSecondaryView!!.width)
                DRAG_EDGE_RIGHT -> mSecondaryView!!.offsetLeftAndRight(mSecondaryView!!.width)
            }
            // Update rects after offset
            initRects()
        }
        
        // Apply initial state (open/closed)
        if (mIsOpenBeforeInit) {
            open(false)
        } else {
            close(false)
        }
    }

    override fun computeScroll() {
        if (mDragHelper!!.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    /**
     * Open the panel to show the secondary view
     */
    fun open(animation: Boolean) {
        mIsOpenBeforeInit = true
        if (mMainView == null || mSecondaryView == null) return
        
        if (animation) {
            mDragHelper!!.smoothSlideViewTo(mMainView!!, mRectMainOpen.left, mRectMainOpen.top)
        } else {
            mDragHelper!!.abort()
            mMainView!!.layout(
                mRectMainOpen.left,
                mRectMainOpen.top,
                mRectMainOpen.right,
                mRectMainOpen.bottom
            )
            mSecondaryView!!.layout(
                mRectSecOpen.left,
                mRectSecOpen.top,
                mRectSecOpen.right,
                mRectSecOpen.bottom
            )
        }
        ViewCompat.postInvalidateOnAnimation(this)
    }

    /**
     * Close the panel to hide the secondary view
     */
    fun close(animation: Boolean) {
        mIsOpenBeforeInit = false
        if (mMainView == null || mSecondaryView == null) return
        
        if (animation) {
            mDragHelper!!.smoothSlideViewTo(mMainView!!, mRectMainClose.left, mRectMainClose.top)
        } else {
            mDragHelper!!.abort()
            mMainView!!.layout(
                mRectMainClose.left,
                mRectMainClose.top,
                mRectMainClose.right,
                mRectMainClose.bottom
            )
            mSecondaryView!!.layout(
                mRectSecClose.left,
                mRectSecClose.top,
                mRectSecClose.right,
                mRectSecClose.bottom
            )
        }
        ViewCompat.postInvalidateOnAnimation(this)
    }

    /**
     * @return Set true for lock the swipe.
     */
    fun dragLock(drag: Boolean) {
        isDragLocked = drag
    }

    private val mainOpenLeft: Int
        get() = when (mDragEdge) {
            DRAG_EDGE_LEFT -> mRectMainClose.left - (mSecondaryView?.width ?: 0)
            DRAG_EDGE_RIGHT -> mRectMainClose.left - (mSecondaryView?.width ?: 0)
            else -> 0
        }
    private val mainOpenTop: Int
        get() {
            return when (mDragEdge) {
                DRAG_EDGE_LEFT -> mRectMainClose.top
                DRAG_EDGE_RIGHT -> mRectMainClose.top
                else -> 0
            }
        }
    private val secOpenLeft: Int
        get() = when (mDragEdge) {
            DRAG_EDGE_LEFT -> mRectMainClose.right
            else -> mRectSecClose.left
        }
    private val secOpenTop: Int
        get() = mRectSecClose.top

    private fun initRects() {
        if (mMainView == null || mSecondaryView == null) return
        
        // close position of main view (current constraint-based position)
        mRectMainClose[mMainView!!.left, mMainView!!.top, mMainView!!.right] = mMainView!!.bottom

        // close position of secondary view (current constraint-based position)
        mRectSecClose[mSecondaryView!!.left, mSecondaryView!!.top, mSecondaryView!!.right] =
            mSecondaryView!!.bottom

        // open position of the main view
        mRectMainOpen[mainOpenLeft, mainOpenTop, mainOpenLeft + mMainView!!.width] =
            mainOpenTop + mMainView!!.height

        // open position of the secondary view
        mRectSecOpen[secOpenLeft, secOpenTop, secOpenLeft + mSecondaryView!!.width] =
            secOpenTop + mSecondaryView!!.height
    }

    private fun couldBecomeClick(ev: MotionEvent): Boolean {
        return isInMainView(ev) && !shouldInitiateADrag()
    }

    private fun isInMainView(ev: MotionEvent): Boolean {
        if (mMainView == null) return false
        val x = ev.x
        val y = ev.y
        val withinVertical = mMainView!!.top <= y && y <= mMainView!!.bottom
        val withinHorizontal = mMainView!!.left <= x && x <= mMainView!!.right
        return withinVertical && withinHorizontal
    }

    private fun shouldInitiateADrag(): Boolean {
        val minDistToInitiateDrag = mDragHelper!!.touchSlop.toFloat()
        return mDragDist >= minDistToInitiateDrag
    }

    private fun accumulateDragDist(ev: MotionEvent) {
        val action = ev.action
        if (action == MotionEvent.ACTION_DOWN) {
            mDragDist = 0f
            return
        }
        val dragged = abs((ev.x - mPrevX).toDouble()).toFloat()
        mDragDist += dragged
    }

    private fun init(context: Context?, attrs: AttributeSet?) {
        if (attrs != null && context != null) {
            val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.SwipeRevealLayout,
                0, 0
            )
            mDragEdge = a.getInteger(R.styleable.SwipeRevealLayout_dragFromEdge, DRAG_EDGE_LEFT)
            isDragLocked = a.getBoolean(R.styleable.SwipeRevealLayout_lockDrag, false)
            a.recycle()
            mMode = MODE_NORMAL
            mMinFlingVelocity = DEFAULT_MIN_FLING_VELOCITY
            mMinDistRequestDisallowParent = DEFAULT_MIN_DIST_REQUEST_DISALLOW_PARENT
        }
        mDragHelper = ViewDragHelper.create(this, 1.0f, mDragHelperCallback)
        mDragHelper?.setEdgeTrackingEnabled(ViewDragHelper.EDGE_ALL)
        mGestureDetector = GestureDetectorCompat(context!!, mGestureListener)
    }

    private val mGestureListener: GestureDetector.OnGestureListener =
        object : SimpleOnGestureListener() {
            var hasDisallowed = false
            override fun onDown(e: MotionEvent): Boolean {
                mIsScrolling = false
                hasDisallowed = false
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                mIsScrolling = true
                return false
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                mIsScrolling = true
                if (parent != null) {
                    val shouldDisallow: Boolean
                    if (!hasDisallowed) {
                        shouldDisallow = distToClosestEdge >= mMinDistRequestDisallowParent
                        if (shouldDisallow) {
                            hasDisallowed = true
                        }
                    } else {
                        shouldDisallow = true
                    }

                    // disallow parent to intercept touch event so that the layout will work
                    // properly on RecyclerView or view that handles scroll gesture.
                    parent.requestDisallowInterceptTouchEvent(shouldDisallow)
                }
                return false
            }
        }
    private val distToClosestEdge: Int
        private get() {
            if (mMainView == null || mSecondaryView == null) return 0
            when (mDragEdge) {
                DRAG_EDGE_LEFT -> {
                    val pivotRight = mRectMainClose.left + mSecondaryView!!.width
                    return min(
                        (
                                mMainView!!.left - mRectMainClose.left).toDouble(),
                        (
                                pivotRight - mMainView!!.left
                                ).toDouble()
                    ).toInt()
                }

                DRAG_EDGE_RIGHT -> {
                    val pivotLeft = mRectMainClose.right - mSecondaryView!!.width
                    return min(
                        (
                                mMainView!!.right - pivotLeft).toDouble(),
                        (
                                mRectMainClose.right - mMainView!!.right
                                ).toDouble()
                    ).toInt()
                }
            }
            return 0
        }
    private val halfwayPivotHorizontal: Int
        private get() {
            if (mSecondaryView == null) return 0
            return if (mDragEdge == DRAG_EDGE_LEFT) {
                mRectMainClose.left - mSecondaryView!!.width / 2
            } else {
                mRectMainClose.right - mSecondaryView!!.width / 2
            }
        }
    private val mDragHelperCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            if (isDragLocked || mMainView == null) return false
            mDragHelper!!.captureChildView(mMainView!!, pointerId)
            return false
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            if (mSecondaryView == null) return child.left
            return when (mDragEdge) {
                DRAG_EDGE_RIGHT -> max(
                    min(left.toDouble(), mRectMainClose.left.toDouble()),
                    (mRectMainClose.left - mSecondaryView!!.width).toDouble()
                ).toInt()

                DRAG_EDGE_LEFT -> max(
                    min(left.toDouble(), mRectMainClose.left.toDouble()),
                    (mRectMainClose.left - mSecondaryView!!.width).toDouble()
                ).toInt()

                else -> child.left
            }
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            if (mMainView == null) return
            val velRightExceeded = pxToDp(xvel.toInt()) >= mMinFlingVelocity
            val velLeftExceeded = pxToDp(xvel.toInt()) <= -mMinFlingVelocity
            val pivotHorizontal: Int = halfwayPivotHorizontal
            when (mDragEdge) {
                DRAG_EDGE_RIGHT -> if (velRightExceeded) {
                    close(true)
                } else if (velLeftExceeded) {
                    open(true)
                } else {
                    if (mMainView!!.right < pivotHorizontal) {
                        open(true)
                    } else {
                        close(true)
                    }
                }

                DRAG_EDGE_LEFT -> if (velRightExceeded) {
                    close(true)
                } else if (velLeftExceeded) {
                    open(true)
                } else {
                    if (mMainView!!.left > pivotHorizontal) {
                        close(true)
                    } else {
                        open(true)
                    }
                }
            }
        }

        override fun onEdgeDragStarted(edgeFlags: Int, pointerId: Int) {
            super.onEdgeDragStarted(edgeFlags, pointerId)
            if (isDragLocked || mMainView == null) {
                return
            }
            val edgeStartLeft = (mDragEdge == DRAG_EDGE_RIGHT
                    && edgeFlags == ViewDragHelper.EDGE_LEFT)
            val edgeStartRight = (mDragEdge == DRAG_EDGE_LEFT
                    && edgeFlags == ViewDragHelper.EDGE_RIGHT)
            if (edgeStartLeft || edgeStartRight) {
                mDragHelper!!.captureChildView(mMainView!!, pointerId)
            }
        }

        override fun onViewPositionChanged(
            changedView: View,
            left: Int,
            top: Int,
            dx: Int,
            dy: Int
        ) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            if (mMode == MODE_SAME_LEVEL && mSecondaryView != null) {
                if (mDragEdge == DRAG_EDGE_LEFT || mDragEdge == DRAG_EDGE_RIGHT) {
                    mSecondaryView!!.offsetLeftAndRight(dx)
                } else {
                    mSecondaryView!!.offsetTopAndBottom(dy)
                }
            }
            ViewCompat.postInvalidateOnAnimation(this@ItemDecor)
        }
    }

    private fun pxToDp(px: Int): Int {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return (px / (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
    }

    companion object {
        private const val SUPER_INSTANCE_STATE = "saved_instance_state_parcelable"
        private const val DEFAULT_MIN_FLING_VELOCITY = 300 // dp per second
        private const val DEFAULT_MIN_DIST_REQUEST_DISALLOW_PARENT = 1 // dp
        const val DRAG_EDGE_LEFT = 0x1
        const val DRAG_EDGE_RIGHT = 0x1 shl 1

        /**
         * The secondary view will be under the main view.
         */
        const val MODE_NORMAL = 0

        /**
         * The secondary view will stick the edge of the main view.
         */
        const val MODE_SAME_LEVEL = 1
    }
}