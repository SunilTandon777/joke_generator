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
     * handle the initial constraint-based positioning and dynamic sizing
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // Let ConstraintLayout do its normal layout first - this handles all constraints
        // including dynamic height matching between views
        super.onLayout(changed, left, top, right, bottom)
        
        // Only proceed if we have both views
        if (mMainView == null || mSecondaryView == null) return
        
        // Store the constraint-based positions as our base positions
        // These positions now reflect the dynamic sizing from ConstraintLayout
        initRects()
        
        // Apply any offset for SAME_LEVEL mode after constraint layout is complete
        if (mMode == MODE_SAME_LEVEL) {
            when (mDragEdge) {
                DRAG_EDGE_LEFT -> {
                    mSecondaryView!!.offsetLeftAndRight(-mSecondaryView!!.width)
                }
                DRAG_EDGE_RIGHT -> {
                    mSecondaryView!!.offsetLeftAndRight(mSecondaryView!!.width)
                }
            }
            // Update rects after offset to reflect the new positions
            initRects()
        }
        
        // Apply initial state (open/closed) based on current positions
        // Only if we're not currently in a drag operation
        if (!isDragging()) {
            if (mIsOpenBeforeInit) {
                open(false)
            } else {
                close(false)
            }
        }
    }

    /**
     * Check if we're currently in a drag operation
     */
    private fun isDragging(): Boolean {
        return mDragHelper?.viewDragState == ViewDragHelper.STATE_DRAGGING ||
               mDragHelper?.viewDragState == ViewDragHelper.STATE_SETTLING
    }

    /**
     * Override onMeasure to ensure proper measurement while maintaining ConstraintLayout's
     * dynamic sizing capabilities
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Let ConstraintLayout handle all the measurement including constraint-based sizing
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        
        // Ensure we have at least 2 children for swipe functionality
        if (childCount < 2) {
            throw RuntimeException("ItemDecor must have two children for swipe functionality")
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
        
        // Calculate the exact open position based on secondary view width and drag direction
        val closedLeft = mRectMainClose.left
        val secondaryWidth = mSecondaryView!!.width
        val targetLeft = when (mDragEdge) {
            DRAG_EDGE_RIGHT -> closedLeft - secondaryWidth  // Move main view LEFT to reveal buttons on RIGHT
            DRAG_EDGE_LEFT -> closedLeft + secondaryWidth   // Move main view RIGHT to reveal buttons on LEFT
            else -> closedLeft
        }
        
        if (animation) {
            mDragHelper!!.smoothSlideViewTo(mMainView!!, targetLeft, mRectMainClose.top)
        } else {
            mDragHelper!!.abort()
            // Position main view at exact open position
            layoutViewSafely(
                mMainView!!,
                targetLeft,
                mRectMainClose.top,
                targetLeft + mMainView!!.width,
                mRectMainClose.bottom
            )
            // Secondary view stays in its original constraint-based position (don't move it)
            // This ensures the action buttons stay visible on the right side
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
            // Position main view back to exact closed position (covering the action buttons)
            layoutViewSafely(
                mMainView!!,
                mRectMainClose.left,
                mRectMainClose.top,
                mRectMainClose.right,
                mRectMainClose.bottom
            )
            // Secondary view stays in its original constraint-based position (don't move it)
            // Action buttons remain in place, just covered by main view
        }
        ViewCompat.postInvalidateOnAnimation(this)
    }

    /**
     * Layout a view safely with bounds checking to prevent layout corruption
     */
    private fun layoutViewSafely(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        // Ensure the bounds are valid
        if (left >= right || top >= bottom) return
        
        // Ensure the view stays within the parent bounds
        val parentWidth = width
        val parentHeight = height
        
        val safeLeft = max(0, min(left, parentWidth - view.measuredWidth))
        val safeTop = max(0, min(top, parentHeight - view.measuredHeight))
        val safeRight = min(parentWidth, max(right, safeLeft + view.measuredWidth))
        val safeBottom = min(parentHeight, max(bottom, safeTop + view.measuredHeight))
        
        view.layout(safeLeft, safeTop, safeRight, safeBottom)
    }

    /**
     * @return Set true for lock the swipe.
     */
    fun dragLock(drag: Boolean) {
        isDragLocked = drag
    }

    /**
     * Manually trigger open for testing - call this to test if swipe works
     */
    fun testOpen() {
        open(true)
    }

    /**
     * Manually trigger close for testing - call this to test if swipe works
     */
    fun testClose() {
        close(true)
    }

    /**
     * Get current drag edge for debugging
     */
    fun getCurrentDragEdge(): Int {
        return mDragEdge
    }

    /**
     * Check if views are properly initialized
     */
    fun isViewsInitialized(): Boolean {
        return mMainView != null && mSecondaryView != null
    }

    /**
     * Get secondary view width for debugging
     */
    fun getSecondaryViewWidth(): Int {
        return mSecondaryView?.width ?: 0
    }

    private val mainOpenLeft: Int
        get() = when (mDragEdge) {
            DRAG_EDGE_LEFT -> mRectMainClose.left + (mSecondaryView?.width ?: 0)
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
        get() = mRectSecClose.left
    private val secOpenTop: Int
        get() = mRectSecClose.top

    private fun initRects() {
        if (mMainView == null || mSecondaryView == null) return
        
        // Store current constraint-based positions as base positions
        // These positions now include any dynamic sizing from constraints
        mRectMainClose.set(
            mMainView!!.left, 
            mMainView!!.top, 
            mMainView!!.right, 
            mMainView!!.bottom
        )

        mRectSecClose.set(
            mSecondaryView!!.left, 
            mSecondaryView!!.top, 
            mSecondaryView!!.right,
            mSecondaryView!!.bottom
        )

        // Calculate open positions based on current (potentially dynamic) sizes
        mRectMainOpen.set(
            mainOpenLeft, 
            mainOpenTop, 
            mainOpenLeft + mMainView!!.width,
            mainOpenTop + mMainView!!.height
        )

        mRectSecOpen.set(
            secOpenLeft, 
            secOpenTop, 
            secOpenLeft + mSecondaryView!!.width,
            secOpenTop + mSecondaryView!!.height
        )
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
        } else {
            // Default values when no attributes
            mDragEdge = DRAG_EDGE_RIGHT  // Default to right for your use case
            isDragLocked = false
        }
        
        mMode = MODE_NORMAL
        mMinFlingVelocity = DEFAULT_MIN_FLING_VELOCITY
        mMinDistRequestDisallowParent = DEFAULT_MIN_DIST_REQUEST_DISALLOW_PARENT
        
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
                    // Distance from current position to closed position or max open position
                    val distToClosed = abs(mMainView!!.left - mRectMainClose.left)
                    val maxOpenLeft = mRectMainClose.left + mSecondaryView!!.width
                    val distToMaxOpen = abs(mMainView!!.left - maxOpenLeft)
                    return min(distToClosed, distToMaxOpen)
                }

                DRAG_EDGE_RIGHT -> {
                    // Distance from current position to closed position or max open position
                    val distToClosed = abs(mMainView!!.left - mRectMainClose.left)
                    val maxOpenLeft = mRectMainClose.left - mSecondaryView!!.width
                    val distToMaxOpen = abs(mMainView!!.left - maxOpenLeft)
                    return min(distToClosed, distToMaxOpen)
                }
            }
            return 0
        }
    private val halfwayPivotHorizontal: Int
        private get() {
            if (mSecondaryView == null) return 0
            return when (mDragEdge) {
                DRAG_EDGE_LEFT -> {
                    // Halfway point between closed and fully open position
                    val closedLeft = mRectMainClose.left
                    val openLeft = mRectMainClose.left + mSecondaryView!!.width
                    closedLeft + (openLeft - closedLeft) / 2
                }
                DRAG_EDGE_RIGHT -> {
                    // Halfway point between closed and fully open position
                    val closedLeft = mRectMainClose.left
                    val openLeft = mRectMainClose.left - mSecondaryView!!.width
                    closedLeft + (openLeft - closedLeft) / 2
                }
                else -> 0
            }
        }
    private val mDragHelperCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            if (isDragLocked || mMainView == null) return false
            // Only allow capturing the main view
            return child == mMainView
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            if (mSecondaryView == null || mMainView == null || child != mMainView) {
                return child.left
            }
            
            // Get the current closed position and secondary view width
            val closedLeft = mRectMainClose.left
            val secondaryWidth = mSecondaryView!!.width
            
            return when (mDragEdge) {
                DRAG_EDGE_RIGHT -> {
                    // For right edge drag (swipe left to reveal), main view moves LEFT
                    // Closed position: normal position
                    // Open position: moved left by secondary width to reveal buttons on right
                    val minAllowedLeft = closedLeft - secondaryWidth  // Fully open (moved left)
                    val maxAllowedLeft = closedLeft                   // Fully closed (normal position)
                    
                    // Clamp the position
                    when {
                        left < minAllowedLeft -> minAllowedLeft
                        left > maxAllowedLeft -> maxAllowedLeft
                        else -> left
                    }
                }

                DRAG_EDGE_LEFT -> {
                    // For left edge drag (swipe right to reveal), main view moves RIGHT  
                    val minAllowedLeft = closedLeft                   // Fully closed (normal position)
                    val maxAllowedLeft = closedLeft + secondaryWidth  // Fully open (moved right)
                    
                    // Clamp the position
                    when {
                        left < minAllowedLeft -> minAllowedLeft
                        left > maxAllowedLeft -> maxAllowedLeft
                        else -> left
                    }
                }

                else -> child.left
            }
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            // Lock vertical movement for main view during horizontal swipe
            if (child == mMainView && mMainView != null) {
                return mRectMainClose.top
            }
            return child.top
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            // Only the main view can be dragged, and only by the width of secondary view
            return if (child == mMainView && mSecondaryView != null) {
                mSecondaryView!!.width
            } else {
                0
            }
        }

        override fun getViewVerticalDragRange(child: View): Int {
            // No vertical drag allowed
            return 0
        }

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            // Force invalidation on state changes
            ViewCompat.postInvalidateOnAnimation(this@ItemDecor)
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            if (mMainView == null || mSecondaryView == null || releasedChild != mMainView) return
            
            val velRightExceeded = pxToDp(xvel.toInt()) >= mMinFlingVelocity
            val velLeftExceeded = pxToDp(xvel.toInt()) <= -mMinFlingVelocity
            
            val currentLeft = mMainView!!.left
            val closedLeft = mRectMainClose.left
            val secondaryWidth = mSecondaryView!!.width
            
            when (mDragEdge) {
                DRAG_EDGE_RIGHT -> {
                    // For right edge drag: swipe LEFT to open, swipe RIGHT to close
                    val openLeft = closedLeft - secondaryWidth
                    val threshold = closedLeft - (secondaryWidth / 2)
                    
                    when {
                        velLeftExceeded -> open(true)    // Fast swipe left = open
                        velRightExceeded -> close(true)  // Fast swipe right = close
                        currentLeft <= threshold -> open(true)   // Past halfway = open
                        else -> close(true)              // Before halfway = close
                    }
                }

                DRAG_EDGE_LEFT -> {
                    // For left edge drag: swipe RIGHT to open, swipe LEFT to close
                    val openLeft = closedLeft + secondaryWidth
                    val threshold = closedLeft + (secondaryWidth / 2)
                    
                    when {
                        velRightExceeded -> open(true)   // Fast swipe right = open
                        velLeftExceeded -> close(true)   // Fast swipe left = close
                        currentLeft >= threshold -> open(true)   // Past halfway = open
                        else -> close(true)              // Before halfway = close
                    }
                }
            }
        }

        override fun onEdgeDragStarted(edgeFlags: Int, pointerId: Int) {
            super.onEdgeDragStarted(edgeFlags, pointerId)
            if (isDragLocked || mMainView == null) {
                return
            }
            
            // For right edge drag, we want to detect swipes from the RIGHT edge of the screen
            // For left edge drag, we want to detect swipes from the LEFT edge of the screen
            val allowEdgeDrag = when (mDragEdge) {
                DRAG_EDGE_RIGHT -> edgeFlags == ViewDragHelper.EDGE_RIGHT  // Swipe from right edge
                DRAG_EDGE_LEFT -> edgeFlags == ViewDragHelper.EDGE_LEFT    // Swipe from left edge
                else -> false
            }
            
            if (allowEdgeDrag) {
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
            
            // Only handle position changes for the main view
            if (changedView != mMainView || mSecondaryView == null) return
            
            // Validate bounds to prevent layout corruption
            val parentWidth = width
            val parentHeight = height
            val viewWidth = changedView.width
            val viewHeight = changedView.height
            
            // Check if view is within parent bounds
            val isWithinParentBounds = left >= -viewWidth && 
                                     left <= parentWidth && 
                                     top >= 0 && 
                                     top + viewHeight <= parentHeight
            
            if (!isWithinParentBounds) {
                // Force safe position reset
                post {
                    close(false)
                }
                return
            }
            
            // Additional boundary check for swipe limits
            val closedLeft = mRectMainClose.left
            val secondaryWidth = mSecondaryView!!.width
            val isWithinSwipeBounds = when (mDragEdge) {
                DRAG_EDGE_RIGHT -> left >= (closedLeft - secondaryWidth) && left <= closedLeft
                DRAG_EDGE_LEFT -> left >= closedLeft && left <= (closedLeft + secondaryWidth)
                else -> true
            }
            
            if (!isWithinSwipeBounds) {
                // Force position back to valid range
                post {
                    if (mIsOpenBeforeInit) {
                        open(false)
                    } else {
                        close(false)
                    }
                }
                return
            }
            
            // Handle SAME_LEVEL mode - secondary view follows main view
            if (mMode == MODE_SAME_LEVEL) {
                when (mDragEdge) {
                    DRAG_EDGE_LEFT, DRAG_EDGE_RIGHT -> {
                        mSecondaryView!!.offsetLeftAndRight(dx)
                    }
                    else -> {
                        mSecondaryView!!.offsetTopAndBottom(dy)
                    }
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

    // Debug and testing methods
    fun testOpen() {
        open(true)
    }

    fun testClose() {
        close(true)
    }

    fun getDebugInfo(): String {
        return buildString {
            appendLine("=== ItemDecor Debug Info ===")
            appendLine("Child count: $childCount")
            appendLine("Main view: ${mMainView != null}")
            appendLine("Secondary view: ${mSecondaryView != null}")
            appendLine("Drag edge: $mDragEdge (1=LEFT, 2=RIGHT)")
            appendLine("Drag locked: $isDragLocked")
            appendLine("Is open before init: $mIsOpenBeforeInit")
            if (mMainView != null) {
                appendLine("Main view position: (${mMainView!!.left}, ${mMainView!!.top})")
                appendLine("Main view size: ${mMainView!!.width} x ${mMainView!!.height}")
            }
            if (mSecondaryView != null) {
                appendLine("Secondary view position: (${mSecondaryView!!.left}, ${mSecondaryView!!.top})")
                appendLine("Secondary view size: ${mSecondaryView!!.width} x ${mSecondaryView!!.height}")
            }
            appendLine("Parent size: $width x $height")
        }
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