package com.aispeech.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.PathInterpolator
import com.aispeech.ailog.AILog

import com.aispeech.widget.base.RefreshViewController
import com.example.scenedemo.R


/**
 * 居中文本流
 * 去除第三方依赖后的ASR动画文本框.
 */
class SpeechASRAnimTextView : View {


    /********************* 下面的参数根据项目实际情况修改  **************************************/

    private var maxWidth: Float = 1000f    //文本框最大宽度

    //AIUtils.getContext().resources.getDimension(R.dimen.asr_text_view_max_width)
    private var minWidth: Float = 78f       //文本框最小宽度

    // AIUtils.getContext().resources.getDimension(R.dimen.asr_text_view_min_width)
    private var height: Float = 80f        //文本框高度

    //AIUtils.getContext().resources.getDimension(R.dimen.asr_text_view_height)
    private var textSize: Float = 45f       //文本框字体大小

    // AIUtils.getContext().resources.getDimension(com.flyme.auto.design.R.dimen.fa_text_body_mini)
    private var textPaddingStartEnd = 0f     //文本框字体离左边距大小
    //  AIUtils.getContext().resources.getDimension(R.dimen.asr_text_padding_start_end)

    private var textColor = 0     //文本颜色值
    private var bgRectColor = 0   //背景框颜色值
    private var bgBorderColor =0  //边框颜色值


    /***********************************************************/


    companion object {
        private const val TAG = "SpeechASRAnimTextView"
    }


    private var currentText: String = ""
    private var oldText: String = ""
    private var hideFinishCallback: () -> Unit = {}
    private var currentRecordId = ""

    private val bgPaint: Paint = Paint()
    private val bgPath: Path = Path()
    private val bgRectPath: Path = Path()
    private val bgRectRadius = height / 2f
    private val bgBorderPaint: Paint = Paint()
    private val bgBorderPath: Path = Path()
    private val borderWidth = 1f
    private val halfBorderWidth = borderWidth / 2f

    private var textTransX = 0f //文字移动的x方向距离
    private var textBaseLineY: Float = 0f
    private var textPaint: Paint = Paint()
    private var textShader: LinearGradient? = null

    private val textWeightMin = 400
    private val textWeightMax = 550
    private val textWeightFeatureSettingsStrPre = "'wght' "
    private var textWeightFeatureSettingsStr = "$textWeightFeatureSettingsStrPre$textWeightMin"

    private var allTextWidth = 0f
    private var maxShowTextWidth: Float = 0f
    private var blurPointPercent = 0.08f //需要模糊的长度比例
    private var blurLen: Float = 0f
    private var blurStartX = bgRectRadius + textPaddingStartEnd

    private var customWidth: Int = 0
    private var customHeight: Int = 0

    private val notCmdExecutedTextMaxAlpha = (0.8f * 255).toInt() //指令未执行的时候，最大的alpha值为0.8 * 255
    private var textAlphaInCmdExecutedState = notCmdExecutedTextMaxAlpha ////指令执行的时候text的alpha值

    private var enterAnim: ValueAnimator? = null
    private var exitAnim: ValueAnimator? = null
    private var textAlphaAnim: ValueAnimator? = null
    private var textShowAnimSet: AnimatorSet? = null
    private var cmdExecutedAnimSet: AnimatorSet? = null

    private var canShowText = false

    private var initialAlpha = 1f
    private var drawTextStartX = bgRectRadius + textPaddingStartEnd

    private var textState: TextState = TextState.NONE

    private val textDrawTaskList = mutableListOf<TextDrawTask>()

    private val refreshViewController = RefreshViewController(1)

    private var pendingSetCmdExecutedStateTask: SetCmdExecutedStateTask? = null
    private var curSetCmdExecutedStateTask: SetCmdExecutedStateTask? = null

    private val viewRefreshCallback = object : RefreshViewController.ViewRefreshCallback {
        override fun invalidateView() {
            //Log.i(TAG, "invalidateView")
            this@SpeechASRAnimTextView.invalidate()
        }
    }

    private var needChangeTextColorOrPos = false //是否需要修改文本颜色或者位置


    private var oneChineseWorldWidth = 0f

    // 创建 LinearGradient 对象，实现 x 方向的 alpha 渐变

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        readAttributes(attrs)
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        readAttributes(attrs)
        init()
    }

    private fun init() {
        bgPaint.color = bgRectColor
        bgPaint.style = Paint.Style.FILL
        bgPaint.isAntiAlias = true

        bgBorderPaint.setARGB(
            Color.alpha(bgBorderColor),
            Color.red(bgBorderColor),
            Color.green(bgBorderColor),
            Color.blue(bgBorderColor)
        )
        bgBorderPaint.style = Paint.Style.STROKE
        bgBorderPaint.strokeWidth = borderWidth
        bgBorderPaint.isAntiAlias = true

        textPaint.color = textColor
        textPaint.textSize = textSize
        textPaint.isAntiAlias = true
        textPaint.style = Paint.Style.FILL_AND_STROKE
        textWeightFeatureSettingsStr = "$textWeightFeatureSettingsStrPre$textWeightMin"
        textPaint.setFontVariationSettings(textWeightFeatureSettingsStr)
        oneChineseWorldWidth = getTextWidth("中")

        val textFontMetrics = textPaint.fontMetrics
        textBaseLineY =
            height / 2f + (textFontMetrics.bottom - textFontMetrics.top) / 2f - textFontMetrics.bottom
        maxShowTextWidth = maxWidth - 2 * (bgRectRadius + textPaddingStartEnd)
        blurLen = maxShowTextWidth * blurPointPercent
    }

    /**
     * 读取xml定义的属性
     */
    private fun readAttributes(attrs:AttributeSet?){
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SpeechASRAnimTextView)
        textColor = ta.getColor(R.styleable.SpeechASRAnimTextView_asrTextColor, Color.parseColor("#181717"))
        bgRectColor = ta.getColor(R.styleable.SpeechASRAnimTextView_asrBgRectColor, Color.TRANSPARENT)
        bgBorderColor = ta.getColor(R.styleable.SpeechASRAnimTextView_asrBgBorderColor, Color.TRANSPARENT)
        textSize = ta.getFloat(R.styleable.SpeechASRAnimTextView_asrTextSize, 45f) //
        maxWidth = ta.getFloat(R.styleable.SpeechASRAnimTextView_asrMaxWidth, 1000f)  //文本框最大宽度
        minWidth = ta.getFloat(R.styleable.SpeechASRAnimTextView_asrMinWidth, 78f)
        height = ta.getFloat(R.styleable.SpeechASRAnimTextView_asrHeight, 80f)
        textPaddingStartEnd = ta.getFloat(R.styleable.SpeechASRAnimTextView_asrTextPaddingStartEnd, 0f)
        AILog.i(TAG,"readAttributes textSize = $textSize textColor=$textColor  bgRectColor=$bgRectColor  bgBorderColor=$bgBorderColor   maxWidth=$maxWidth  minWidth=$minWidth height=$height  textPaddingStartEnd=$textPaddingStartEnd" )
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        needChangeTextColorOrPos = true
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (customWidth > 0 && customHeight > 0) {
            setMeasuredDimension(customWidth, customHeight)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //边框绘制
        bgPaint.color = bgRectColor
        canvas.drawPath(bgPath, bgPaint)

        bgBorderPaint.setARGB(
            Color.alpha(bgBorderColor),
            Color.red(bgBorderColor),
            Color.green(bgBorderColor),
            Color.blue(bgBorderColor)
        )
        canvas.drawPath(bgBorderPath, bgBorderPaint)

        //上屏文字绘制
        canvas.save()
        canvas.clipPath(bgRectPath)
        when (textState) {
            TextState.CONTAINS -> drawContainsText(canvas)
            TextState.OTHER -> drawOtherText(canvas)
            TextState.CMD_EXECUTED -> drawCmdExecutedText(canvas)
            else -> {
                //do nothing
            }
        }
        canvas.restore()
    }

    /**
     * 绘制包含文本
     */
    private fun drawContainsText(canvas: Canvas) {
        val taskLen = textDrawTaskList.size
        if (taskLen == 0) {
            return
        }
        val isBgMaxWidth = measuredWidth >= maxWidth.toInt()
        drawTextStartX = if (isBgMaxWidth) {
            bgRectRadius + textPaddingStartEnd + maxShowTextWidth - textTransX
        } else {
            bgRectRadius + textPaddingStartEnd
        }
        textPaint.setFontVariationSettings(textWeightFeatureSettingsStr)
        if (!needChangeTextColorOrPos && textShader != null) {
            textPaint.shader = textShader
            canvas.drawText(
                currentText,
                drawTextStartX,
                textBaseLineY,
                textPaint
            )
            return
        }
        var fullAlphaSumTextWidth = 0f
        var fullAlphaSumText = ""
        var notFullAlpha = 0
        var firstNotFullAlphaIndex = taskLen
        for (i in 0 until taskLen) {
            val task = textDrawTaskList[i]
            if (task.textAlpha < notCmdExecutedTextMaxAlpha) {
                notFullAlpha = task.textAlpha
                firstNotFullAlphaIndex = i
                break
            }
            fullAlphaSumTextWidth += task.textWidth
            fullAlphaSumText += task.text
        }
        /*        Log.i(TAG, "drawContainsText, drawTextStartX: $drawTextStartX, textTransX: $textTransX, " +
                        "firstNotFullAlphaIndex: $firstNotFullAlphaIndex, notFullAlpha: $notFullAlpha")*/
        val textOriAlphaFactor = Color.alpha(textColor) / 255f
        val textMaxRealAlpha = (notCmdExecutedTextMaxAlpha * textOriAlphaFactor).toInt()
        val notFullRealAlpha = (notFullAlpha * textOriAlphaFactor).toInt()
        if (isBgMaxWidth) {
            val blurStartPercent = (blurStartX - drawTextStartX) / allTextWidth
            if (firstNotFullAlphaIndex == 0 || firstNotFullAlphaIndex == taskLen) { //全部都没有notCmdExecutedTextMaxAlpha5的或者全部是notCmdExecutedTextMaxAlpha
                val realAlpha = if (firstNotFullAlphaIndex == 0) {
                    notFullRealAlpha
                } else {
                    textMaxRealAlpha
                }
                val textShaderColors = intArrayOf(
                    Color.argb(
                        0,
                        Color.red(textColor),
                        Color.green(textColor),
                        Color.blue(textColor)
                    ),
                    Color.argb(
                        0,
                        Color.red(textColor),
                        Color.green(textColor),
                        Color.blue(textColor)
                    ),
                    Color.argb(
                        realAlpha,
                        Color.red(textColor),
                        Color.green(textColor),
                        Color.blue(textColor)
                    ),
                    Color.argb(
                        realAlpha,
                        Color.red(textColor),
                        Color.green(textColor),
                        Color.blue(textColor)
                    )
                )
                val textShaderPositions = floatArrayOf(
                    0f,
                    blurStartPercent,
                    blurStartPercent + blurPointPercent,
                    1f
                )
                textShader = LinearGradient(
                    drawTextStartX, 0f, drawTextStartX + allTextWidth, height,
                    textShaderColors, textShaderPositions, Shader.TileMode.CLAMP
                )
                textPaint.shader = textShader
                canvas.drawText(
                    currentText,
                    drawTextStartX,
                    textBaseLineY,
                    textPaint
                )
            } else { //部分notCmdExecutedTextMaxAlpha，部分不是
                var hitBlurStartIndex = 0
                var hitBlurEndIndex = 0
                var sumTextWidth = 0f
                for (i in 0 until taskLen) {
                    val task = textDrawTaskList[i]
                    val startX = sumTextWidth
                    val endX = sumTextWidth + task.textWidth
                    if (startX <= blurStartX && endX > blurStartX) {
                        hitBlurStartIndex = i
                    }
                    if (startX < blurStartX + blurLen && endX >= blurStartX + blurLen) {
                        hitBlurEndIndex = i
                        break
                    }
                    sumTextWidth += task.textWidth
                }
                //Log.i(TAG, "drawContainsText, BgMaxWidth hitBlurStartIndex: $hitBlurStartIndex, hitBlurEndIndex: $hitBlurEndIndex")
                if (firstNotFullAlphaIndex > hitBlurEndIndex) { //模糊处理的全部是alpha为notCmdExecutedTextMaxAlpha的
                    //为了解决多段alpha的文字颜色的时候出现canvas绘制折痕的问题，对2notCmdExecutedTextMaxAlpha文字和非notCmdExecutedTextMaxAlpha文字分别绘制。
                    val fullAlphaTextShaderColors = intArrayOf(
                        Color.argb(
                            0,
                            Color.red(textColor),
                            Color.green(textColor),
                            Color.blue(textColor)
                        ),
                        Color.argb(
                            0,
                            Color.red(textColor),
                            Color.green(textColor),
                            Color.blue(textColor)
                        ),
                        Color.argb(
                            textMaxRealAlpha,
                            Color.red(textColor),
                            Color.green(textColor),
                            Color.blue(textColor)
                        ),
                        Color.argb(
                            textMaxRealAlpha,
                            Color.red(textColor),
                            Color.green(textColor),
                            Color.blue(textColor)
                        )
                    )

                    val fullAlphaTextShaderPositions = floatArrayOf(
                        0f,
                        blurStartPercent,
                        blurStartPercent + blurPointPercent,
                        1f
                    )
                    textShader = LinearGradient(
                        drawTextStartX,
                        0f,
                        drawTextStartX + fullAlphaSumTextWidth,
                        height,
                        fullAlphaTextShaderColors,
                        fullAlphaTextShaderPositions,
                        Shader.TileMode.CLAMP
                    )
                    textPaint.shader = textShader
                    canvas.drawText(
                        fullAlphaSumText,
                        drawTextStartX,
                        textBaseLineY,
                        textPaint
                    )

                    val notFullAlphaTextShaderColors = intArrayOf(
                        Color.argb(
                            notFullRealAlpha,
                            Color.red(textColor),
                            Color.green(textColor),
                            Color.blue(textColor)
                        ),
                        Color.argb(
                            notFullRealAlpha,
                            Color.red(textColor),
                            Color.green(textColor),
                            Color.blue(textColor)
                        )
                    )

                    val notFullAlphaTextShaderPositions = floatArrayOf(
                        0f,
                        1f
                    )
                    val notFullAlphaTextStartX = drawTextStartX + fullAlphaSumTextWidth
                    textShader = LinearGradient(
                        notFullAlphaTextStartX,
                        0f,
                        drawTextStartX + allTextWidth,
                        height,
                        notFullAlphaTextShaderColors,
                        notFullAlphaTextShaderPositions,
                        Shader.TileMode.CLAMP
                    )
                    textPaint.shader = textShader
                    canvas.drawText(
                        currentText.replace(fullAlphaSumText, ""),
                        notFullAlphaTextStartX,
                        textBaseLineY,
                        textPaint
                    )

                } else {//模糊处理的全部不是alpha为notCmdExecutedTextMaxAlpha和部分是alpha为notCmdExecutedTextMaxAlpha的，这里简单处理部分blur包含部分notCmdExecutedTextMaxAlpha，部分不是notCmdExecutedTextMaxAlpha的情况，因为会引起绘制割裂问题
                    val textShaderColors = intArrayOf(
                        Color.argb(
                            0,
                            Color.red(textColor),
                            Color.green(textColor),
                            Color.blue(textColor)
                        ),
                        Color.argb(
                            0,
                            Color.red(textColor),
                            Color.green(textColor),
                            Color.blue(textColor)
                        ),
                        Color.argb(
                            notFullRealAlpha,
                            Color.red(textColor),
                            Color.green(textColor),
                            Color.blue(textColor)
                        ),
                        Color.argb(
                            notFullRealAlpha,
                            Color.red(textColor),
                            Color.green(textColor),
                            Color.blue(textColor)
                        )
                    )
                    val textShaderPositions = floatArrayOf(
                        0f,
                        blurStartPercent,
                        blurStartPercent + blurPointPercent,
                        1f
                    )
                    textShader = LinearGradient(
                        drawTextStartX, 0f, drawTextStartX + allTextWidth, height,
                        textShaderColors, textShaderPositions, Shader.TileMode.CLAMP
                    )
                    textPaint.shader = textShader
                    canvas.drawText(
                        currentText,
                        drawTextStartX,
                        textBaseLineY,
                        textPaint
                    )
                }
            }
        } else {
            if (firstNotFullAlphaIndex == 0 || firstNotFullAlphaIndex == taskLen) { //全部都没有notCmdExecutedTextMaxAlpha的 或者全部都是notCmdExecutedTextMaxAlpha的
                val realAlpha = if (firstNotFullAlphaIndex == 0) {
                    notFullRealAlpha
                } else {
                    textMaxRealAlpha
                }
                val textShaderColors = intArrayOf(
                    Color.argb(
                        realAlpha,
                        Color.red(textColor),
                        Color.green(textColor),
                        Color.blue(textColor)
                    ),
                    Color.argb(
                        realAlpha,
                        Color.red(textColor),
                        Color.green(textColor),
                        Color.blue(textColor)
                    )
                )
                val textShaderPositions = floatArrayOf(
                    0f,
                    1f
                )
                textShader = LinearGradient(
                    drawTextStartX, 0f, drawTextStartX + allTextWidth, height,
                    textShaderColors, textShaderPositions, Shader.TileMode.CLAMP
                )
                textPaint.shader = textShader
                canvas.drawText(
                    currentText,
                    drawTextStartX,
                    textBaseLineY,
                    textPaint
                )
            } else { //部分notCmdExecutedTextMaxAlpha，部分不是
                //为了解决多段alpha的文字颜色的时候出现canvas绘制折痕的问题，对每一段文字分别绘制。
                var sumTextWidth = 0f
                for (i in 0 until taskLen) {
                    val task = textDrawTaskList[i]
                    val startX = drawTextStartX + sumTextWidth
                    val taskRealTextAlpha = (task.textAlpha * textOriAlphaFactor).toInt()
                    val textShaderColors = intArrayOf(
                        Color.argb(
                            taskRealTextAlpha,
                            Color.red(textColor),
                            Color.green(textColor),
                            Color.blue(textColor)
                        ),
                        Color.argb(
                            taskRealTextAlpha,
                            Color.red(textColor),
                            Color.green(textColor),
                            Color.blue(textColor)
                        )
                    )
                    val textShaderPositions = floatArrayOf(
                        0f,
                        1f
                    )
                    textShader = LinearGradient(
                        startX, 0f, startX + task.textWidth, height,
                        textShaderColors, textShaderPositions, Shader.TileMode.CLAMP
                    )
                    textPaint.shader = textShader
                    canvas.drawText(
                        task.text,
                        startX,
                        textBaseLineY,
                        textPaint
                    )
                    sumTextWidth += task.textWidth
                }
            }
        }
    }

    /**
     * 绘制其他文本
     */
    private fun drawOtherText(canvas: Canvas) {
        val isBgMaxWidth = measuredWidth >= maxWidth.toInt()
        drawTextStartX = if (isBgMaxWidth) {
            bgRectRadius + textPaddingStartEnd - textTransX
        } else {
            bgRectRadius + textPaddingStartEnd
        }
        textPaint.setFontVariationSettings(textWeightFeatureSettingsStr)
        if (!needChangeTextColorOrPos && textShader != null) {
            textPaint.shader = textShader

            canvas.drawText(
                currentText,
                drawTextStartX,
                textBaseLineY,
                textPaint
            )
            return
        }
        val textRealMaxAlpha = (notCmdExecutedTextMaxAlpha * Color.alpha(textColor) / 255f).toInt()
        val textShaderColors: IntArray
        val textShaderPositions: FloatArray
        if (isBgMaxWidth) {
            val blurStartPercent = (blurStartX - drawTextStartX) / allTextWidth
            textShaderColors = intArrayOf(
                Color.argb(
                    0,
                    Color.red(textColor),
                    Color.green(textColor),
                    Color.blue(textColor)
                ),
                Color.argb(
                    0,
                    Color.red(textColor),
                    Color.green(textColor),
                    Color.blue(textColor)
                ),
                Color.argb(
                    textRealMaxAlpha,
                    Color.red(textColor),
                    Color.green(textColor),
                    Color.blue(textColor)
                ),
                Color.argb(
                    textRealMaxAlpha,
                    Color.red(textColor),
                    Color.green(textColor),
                    Color.blue(textColor)
                ),
            )
            textShaderPositions = floatArrayOf(
                0f,
                blurStartPercent,
                blurStartPercent + blurPointPercent,
                1f
            )
        } else {
            textShaderColors = intArrayOf(
                Color.argb(
                    textRealMaxAlpha,
                    Color.red(textColor),
                    Color.green(textColor),
                    Color.blue(textColor)
                ),
                Color.argb(
                    textRealMaxAlpha,
                    Color.red(textColor),
                    Color.green(textColor),
                    Color.blue(textColor)
                ),
            )
            textShaderPositions = floatArrayOf(
                0f,
                1f
            )
        }
        textShader = LinearGradient(
            drawTextStartX, 0f, drawTextStartX + allTextWidth, height,
            textShaderColors, textShaderPositions, Shader.TileMode.CLAMP
        )

        textPaint.shader = textShader
        canvas.drawText(
            currentText,
            drawTextStartX,
            textBaseLineY,
            textPaint
        )
    }

    private fun drawCmdExecutedText(canvas: Canvas) {
        val realAlpha = (textAlphaInCmdExecutedState / 255f * Color.alpha(textColor)).toInt()
        val isBgMaxWidth = measuredWidth >= maxWidth.toInt()
        textPaint.setFontVariationSettings(textWeightFeatureSettingsStr)
        if (isBgMaxWidth) {
            if (!needChangeTextColorOrPos) {
                textPaint.shader = textShader
                canvas.drawText(
                    currentText,
                    drawTextStartX,
                    textBaseLineY,
                    textPaint
                )
                return
            }
            val blurStartPercent = (blurStartX - drawTextStartX) / allTextWidth
            val textShaderColors = intArrayOf(
                Color.argb(
                    0,
                    Color.red(textColor),
                    Color.green(textColor),
                    Color.blue(textColor)
                ),
                Color.argb(
                    0,
                    Color.red(textColor),
                    Color.green(textColor),
                    Color.blue(textColor)
                ),
                Color.argb(
                    realAlpha,
                    Color.red(textColor),
                    Color.green(textColor),
                    Color.blue(textColor)
                ),
                Color.argb(
                    realAlpha,
                    Color.red(textColor),
                    Color.green(textColor),
                    Color.blue(textColor)
                ),
            )
            val textShaderPositions = floatArrayOf(
                0f,
                blurStartPercent,
                blurStartPercent + blurPointPercent,
                1f
            )
            textShader = LinearGradient(
                drawTextStartX, 0f, drawTextStartX + allTextWidth, height,
                textShaderColors, textShaderPositions, Shader.TileMode.CLAMP
            )
            textPaint.shader = textShader
            canvas.drawText(
                currentText,
                drawTextStartX,
                textBaseLineY,
                textPaint
            )
        } else {
            textPaint.shader = null
            textPaint.setARGB(
                realAlpha,
                Color.red(textColor),
                Color.green(textColor),
                Color.blue(textColor)

            )
            canvas.drawText(
                currentText,
                drawTextStartX,
                textBaseLineY,
                textPaint
            )
        }
    }

    /**
     * 设置文字
     */
    fun setText(text: String) {
        Log.i(TAG, "setText: $text")
        if (exitAnim?.isRunning == true) {
            textDrawTaskList.clear()
            val task = TextDrawTask(text)
            textDrawTaskList.add(task)
            return
        }
        if (text == currentText) {
            return
        }
        cancelAnimSet(cmdExecutedAnimSet)
        textWeightFeatureSettingsStr = "$textWeightFeatureSettingsStrPre$textWeightMin"
        oldText = currentText
        currentText = text
        allTextWidth = getTextWidth(text)
        val isNew = oldText.isEmpty()
        val oldTextContainsNew = !isNew && text.startsWith(oldText)
        val newText = if (isNew) {
            text
        } else if (oldTextContainsNew) {
            text.substring(oldText.length)
        } else {
            textDrawTaskList.clear()
            text
        }
        val task = TextDrawTask(newText)
        textDrawTaskList.add(task)
        if (isNew) {
            doEnterAnim()
        }
        Log.i(
            TAG, "setText, isNew: $isNew, oldTextContainsNew: $oldTextContainsNew, " +
                    "newText: $newText, currentText: $currentText, oldText: $oldText"
        )
        if (isNew || oldTextContainsNew) {
            textState = TextState.CONTAINS
            doShowTextAnimaWhenContains(newText)
        } else {
            textState = TextState.OTHER
            task.textAlpha = notCmdExecutedTextMaxAlpha //防止闪烁问题，初始化alpha为notCmdExecutedTextMaxAlpha
            doShowOtherTextAnima()
        }
    }

    fun updateRecordId(recordId: String) {
        Log.i(TAG, "updateRecordId: $recordId")
        currentRecordId = recordId
    }

    fun setAsrCmdExecutedState(input: String, recordId: String) {
        Log.i(TAG, "setAsrCmdExecutedState, input: $input, recordId: $recordId")
        if (currentText.isEmpty() || visibility != VISIBLE) {
            return
        }
        val isSameInput =
            recordId == currentRecordId || (input.isNotEmpty() && input == currentText)
        Log.i(
            TAG,
            "setAsrCmdExecutedState, input: $input, recordId: $recordId, isSameInput: $isSameInput"
        )
        //如果正在执行的命令和当前命令相同，则不执行
        if (textState == TextState.CMD_EXECUTED
            && curSetCmdExecutedStateTask?.recordId == recordId
            && isSameInput
        ) {
            Log.w(TAG, "setAsrCmdExecuteState, hasSetCmdExecutedState: true")
            return
        }
        var canDoAnimImmediate = false
        if (isSameInput
            && enterAnim?.isRunning != true
            && textAlphaAnim?.isRunning != true
            && textShowAnimSet?.isRunning != true
            && exitAnim?.isRunning != true
        ) {
            canDoAnimImmediate = true
        }
        val task = SetCmdExecutedStateTask(input, recordId)
        Log.i(TAG, "setAsrCmdExecuteState, canDoAnimImmediate: $canDoAnimImmediate, task: $task")
        if (canDoAnimImmediate) {
            doSetCmdExecutedStateAnim(task)
        } else {
            pendingSetCmdExecutedStateTask = task
        }
    }

    private fun canDoSetCmdExecutedStateAnim() {
        pendingSetCmdExecutedStateTask?.let { task ->
            val isSameInput =
                task.recordId == currentRecordId || (task.input.isNotEmpty() && task.input == currentText)
            if (!isSameInput) {
                pendingSetCmdExecutedStateTask = null
                Log.w(TAG, "canDoSetCmdExecutedStateAnim false, not the same input: $task")
                return
            }
            if (enterAnim?.isRunning != true
                && textAlphaAnim?.isRunning != true
                && textShowAnimSet?.isRunning != true
                && exitAnim?.isRunning != true
            ) {
                doSetCmdExecutedStateAnim(task)
            }
        }
    }

    private fun doSetCmdExecutedStateAnim(task: SetCmdExecutedStateTask) {
        Log.i(TAG, "doSetCmdExecutedStateAnim, input: ${task.input}, recordId: ${task.recordId}")
        curSetCmdExecutedStateTask = task
        textState = TextState.CMD_EXECUTED
        textAlphaInCmdExecutedState = notCmdExecutedTextMaxAlpha
        textWeightFeatureSettingsStr = "$textWeightFeatureSettingsStrPre$textWeightMin"
        cancelAnim(enterAnim)
        cancelAnim(textAlphaAnim)
        cancelAnimSet(textShowAnimSet)
        cancelAnimSet(cmdExecutedAnimSet)
        val textStrokeWidthAnim = ValueAnimator.ofInt(textWeightMin, textWeightMax).apply {
            duration = 150
            interpolator = PathInterpolator(0.33f, 0f, 0.67f, 1f)
        }
        textStrokeWidthAnim.addUpdateListener {
            val textWeight = it.animatedValue as Int
            textWeightFeatureSettingsStr = "$textWeightFeatureSettingsStrPre$textWeight"
            if (!isEnableReduceAnimFrameRate) {
                this@SpeechASRAnimTextView.invalidate()
            }
        }

        val textAlphaAnim = ValueAnimator.ofInt(notCmdExecutedTextMaxAlpha, 255).apply {
            duration = 150
            interpolator = PathInterpolator(0.33f, 0f, 0.67f, 1f)
        }
        textAlphaAnim.addUpdateListener {
            textAlphaInCmdExecutedState = it.animatedValue as Int
            needChangeTextColorOrPos = true
            if (!isEnableReduceAnimFrameRate) {
                this@SpeechASRAnimTextView.invalidate()
            }
        }
        cmdExecutedAnimSet = AnimatorSet().apply {
            playTogether(textStrokeWidthAnim, textAlphaAnim)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    needChangeTextColorOrPos = true
                    pendingSetCmdExecutedStateTask = null
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    needChangeTextColorOrPos = false
                }
            })
        }
        cmdExecutedAnimSet?.start()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        initialAlpha = if (enabled) {
            1f
        } else {
            0.5f
        }
        if (exitAnim?.isRunning != true) {
            this.alpha = initialAlpha
        }
    }

    fun show() {
        canShowText = true
        if (isEnableReduceAnimFrameRate) {
            unregisterViewRefreshCallback()
            registerViewRefreshCallback()
        }
    }

    fun hide(callback: () -> Unit) {
        val isVisible = visibility == VISIBLE
        if (!isVisible) {
            return
        }
        canShowText = false
        hideFinishCallback = callback
        cancelAllAnim()
        doExitAnim()
    }

    /**
     * 退出动画正在执行需要重新展示asr
     */
    fun needAnewShowText(callback: () -> Unit) {
        if (exitAnim?.isRunning == true) {
            callback.invoke()
        }
    }

    private fun reset() {
        oldText = ""
        currentText = ""
        allTextWidth = 0f
        textTransX = 0f
        textState = TextState.NONE
        textWeightFeatureSettingsStr = "$textWeightFeatureSettingsStrPre$textWeightMin"
        textAlphaInCmdExecutedState = notCmdExecutedTextMaxAlpha
        drawTextStartX = bgRectRadius + textPaddingStartEnd
        pendingSetCmdExecutedStateTask = null
        curSetCmdExecutedStateTask = null
        setCustomSize(minWidth, height)
        requestLayout()
    }

    /**
     * 第一次进入动画，首次展示
     */
    private fun doEnterAnim() {
        this.alpha = initialAlpha
        enterAnim = ValueAnimator.ofInt(0, 255)
        enterAnim!!.duration = 150
        enterAnim!!.interpolator = PathInterpolator(0.33f, 0f, 0.66f, 1f)
        bgPaint.alpha = 0
        enterAnim!!.addUpdateListener {
            bgPaint.alpha = it.animatedValue as Int
        }
        enterAnim!!.start()
    }

    /**
     * 持续上屏动画
     */
    private fun doShowTextAnimaWhenContains(newText: String) {
        cancelAnimSet(textShowAnimSet)
        //新文字透明度动画
        val duration = getShowTextDuration(newText)
        val showedTextWidth = measuredWidth - bgRectRadius - textPaddingStartEnd - drawTextStartX
        val textTransXAnim = ValueAnimator.ofFloat(showedTextWidth, allTextWidth)
        textTransXAnim.duration = duration
        textTransXAnim.interpolator = PathInterpolator(0.2f, 0.3f, 0.6f, 1f)
        textTransXAnim.addUpdateListener {
            textTransX = it.animatedValue as Float
            needChangeTextColorOrPos = true
            if (!isEnableReduceAnimFrameRate) {
                invalidate()
            }
        }

        //边框path动画
        val startWidth = measuredWidth.toFloat()
        val targetWidth = allTextWidth + minWidth
        val bgWidthAnim = ValueAnimator.ofFloat(startWidth, targetWidth)
        bgWidthAnim.duration = duration
        bgWidthAnim.interpolator = PathInterpolator(0.2f, 0.3f, 0.6f, 1f)
        bgWidthAnim.addUpdateListener {
            var realTimeWidth = it.animatedValue as Float
            if (realTimeWidth >= maxWidth) {
                realTimeWidth = maxWidth
            }
            bgPath.reset()
            bgPath.addRoundRect(
                borderWidth,
                borderWidth,
                realTimeWidth - borderWidth,
                height - borderWidth,
                bgRectRadius,
                bgRectRadius,
                Path.Direction.CW
            )
            bgRectPath.reset()
            bgRectPath.addRect(
                bgRectRadius + textPaddingStartEnd,
                0f,
                realTimeWidth - bgRectRadius - textPaddingStartEnd,
                height,
                Path.Direction.CW
            )
            bgBorderPath.reset()
            bgBorderPath.addRoundRect(
                halfBorderWidth,
                halfBorderWidth,
                realTimeWidth - halfBorderWidth,
                height - halfBorderWidth,
                bgRectRadius,
                bgRectRadius,
                Path.Direction.CW
            )
            setCustomSize(realTimeWidth, height)
        }
        textShowAnimSet = AnimatorSet()
        textShowAnimSet!!.play(textTransXAnim).with(bgWidthAnim)
        textShowAnimSet!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                needChangeTextColorOrPos = false
                canDoSetCmdExecutedStateAnim()
            }

            override fun onAnimationCancel(animation: Animator) {
                needChangeTextColorOrPos = false
            }

            override fun onAnimationStart(
                animation: Animator,
                isReverse: Boolean
            ) {
                needChangeTextColorOrPos = true
            }
        })
        textShowAnimSet!!.start()
        doTextAlphaAnim()
    }

    val isEnableReduceAnimFrameRate = false; //是否开启降低动画帧率

    private fun doTextAlphaAnim() {
        if (textAlphaAnim?.isRunning == true) {
            return
        }
        cancelAnim(textAlphaAnim)
        textAlphaAnim = ValueAnimator.ofInt(0, notCmdExecutedTextMaxAlpha)
        textAlphaAnim!!.duration = 300
        textAlphaAnim!!.interpolator = PathInterpolator(0.33f, 0f, 0.66f, 1f)
        textAlphaAnim!!.addUpdateListener {
            val textAlpha = it.animatedValue as Int
            textDrawTaskList.forEach {
                if (it.textAlpha != notCmdExecutedTextMaxAlpha) {
                    it.textAlpha = textAlpha
                }
            }
            needChangeTextColorOrPos = true
            if (!isEnableReduceAnimFrameRate) {
                invalidate()
            }
        }
        textAlphaAnim!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (textShowAnimSet?.isRunning != true) {
                    needChangeTextColorOrPos = false
                    canDoSetCmdExecutedStateAnim()
                }
            }

            override fun onAnimationStart(
                animation: Animator,
                isReverse: Boolean
            ) {
                needChangeTextColorOrPos = true
            }
        })
        textAlphaAnim!!.start()
    }

    /**
     * asr文字上屏动画
     */
    private fun doShowOtherTextAnima() {
        cancelAnimSet(textShowAnimSet)
        cancelAnim(textAlphaAnim)
        val startWidth = measuredWidth.toFloat()
        val targetWidth = allTextWidth + minWidth
        textTransX = 0f
        val duration = 300L
        //边框path动画
        var bgWidthAnim: ValueAnimator? = null
        //如果宽度没变化，则不进行动画
        if (startWidth != targetWidth && !(targetWidth >= maxWidth && startWidth >= maxWidth)) {
            bgWidthAnim = ValueAnimator.ofFloat(startWidth, targetWidth)
            bgWidthAnim.duration = duration
            bgWidthAnim.interpolator =
                if (startWidth > targetWidth) {
                    PathInterpolator(0.2f, 0.6f, 0.1f, 1f)
                } else {
                    PathInterpolator(0.2f, 0.3f, 0.6f, 1f)
                }
            bgWidthAnim.addUpdateListener {
                needChangeTextColorOrPos = true
                var realTimeWidth = it.animatedValue as Float
                if (realTimeWidth >= maxWidth) {
                    realTimeWidth = maxWidth
                }
                bgPath.reset()
                bgPath.addRoundRect(
                    borderWidth,
                    borderWidth,
                    realTimeWidth - borderWidth,
                    height - borderWidth,
                    bgRectRadius,
                    bgRectRadius,
                    Path.Direction.CW
                )
                bgRectPath.reset()
                bgRectPath.addRect(
                    bgRectRadius + textPaddingStartEnd,
                    0f,
                    realTimeWidth - bgRectRadius - textPaddingStartEnd,
                    height,
                    Path.Direction.CW
                )
                bgBorderPath.reset()
                bgBorderPath.addRoundRect(
                    halfBorderWidth,
                    halfBorderWidth,
                    realTimeWidth - halfBorderWidth,
                    height - halfBorderWidth,
                    bgRectRadius,
                    bgRectRadius,
                    Path.Direction.CW
                )
                setCustomSize(realTimeWidth, height)
            }
        }

        //如果新文字超过了最大的显示宽度，则需要执行新文字位移动画
        var lastTextTransXAnim: ValueAnimator? = null
        if (allTextWidth > maxShowTextWidth) {
            val isNewContainsParkOfOldWhenAllMaxWidth =
                isNewContainsParkOfOldWhenAllMaxWidth(currentText, oldText, targetWidth)
            textTransX = if (isNewContainsParkOfOldWhenAllMaxWidth) {
                bgRectRadius + textPaddingStartEnd - drawTextStartX
            } else {
                0f
            }
            val targetTansX = allTextWidth - maxShowTextWidth
            lastTextTransXAnim = ValueAnimator.ofFloat(textTransX, targetTansX)
            lastTextTransXAnim.duration = duration
            lastTextTransXAnim.interpolator = PathInterpolator(0.2f, 0.3f, 0.6f, 1f)
            lastTextTransXAnim.addUpdateListener {
                textTransX = it.animatedValue as Float
                needChangeTextColorOrPos = true
                if (!isEnableReduceAnimFrameRate) {
                    invalidate()
                }
            }
        }
        if (bgWidthAnim == null && lastTextTransXAnim == null) {
            return
        }
        textShowAnimSet = AnimatorSet()
        bgWidthAnim?.let { bgAnim ->
            lastTextTransXAnim?.let { textTransXAnim ->
                textShowAnimSet!!.play(bgAnim).before(textTransXAnim)
            } ?: run {
                textShowAnimSet!!.play(bgAnim)
            }
        } ?: run {
            lastTextTransXAnim?.let { textTransXAnim ->
                textShowAnimSet!!.play(textTransXAnim)
            }
        }
        textShowAnimSet!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                needChangeTextColorOrPos = true
            }

            override fun onAnimationEnd(animation: Animator) {
                needChangeTextColorOrPos = false
                canDoSetCmdExecutedStateAnim()
            }

            override fun onAnimationCancel(animation: Animator) {
                needChangeTextColorOrPos = false
            }
        })
        textShowAnimSet!!.start()
    }

    /**
     * 新text是否包含部分旧text，且在两者都超长的情况下，比如：
     * 下午冲突的最新动态以及美国再起中的丽丽关系目前的搜
     * 下午冲突的最新动态以及美国再起中的丽丽关系目前的生活
     */
    private fun isNewContainsParkOfOldWhenAllMaxWidth(
        newText: String,
        oldText: String,
        targetWidth: Float
    ): Boolean {
        //如果旧text还没绘制超出左边显示边界，则不做处理
        if (drawTextStartX >= bgRectRadius + textPaddingStartEnd) {
            return false
        }
        //如果后面的新text没有超出最大长度，也不用处理
        if (targetWidth <= maxWidth) {
            return false
        }
        //左边超出显示区域的长度
        val outOfShowRectWidth = bgRectRadius + textPaddingStartEnd - drawTextStartX
        var minTextSize = (outOfShowRectWidth / oneChineseWorldWidth).toInt()
        if (minTextSize == 0) {
            minTextSize = 1
        }
        val oldTextSize = oldText.length
        val newTextSize = newText.length
        if (oldTextSize <= minTextSize || newTextSize <= minTextSize) {
            return false
        }
        //出现在显示区域的最小index
        var minStrIndexInShowRect = 0
        for (i in minTextSize until oldTextSize) {
            val textWidth = getTextWidth(oldText.substring(0, i))
            if (textWidth >= outOfShowRectWidth) {
                minStrIndexInShowRect = i
                break
            }
        }
        if (minStrIndexInShowRect >= newTextSize - 1) {
            return false
        }
        //然后看看显示区域之前的oldText和newText是否相同，如果相同，则说明newText不需要从头展示了。
        val oldTextOutOffShowRect = oldText.substring(0, minStrIndexInShowRect)
        val newTextOutOffShowRect = newText.substring(0, minStrIndexInShowRect)
        return oldTextOutOffShowRect == newTextOutOffShowRect
    }

    /**
     * 根据新字的长度获取对应不同的动画时长，解决文字滚动太快的问题
     */
    private fun getShowTextDuration(newText: String): Long {

        val len = newText.length
        return if (len <= 6) {
            800
        } else if (len <= 12) {
            1000
        } else {
            1200
        }
    }

    private fun cancelAllAnim() {
        cancelAnim(exitAnim)
        cancelAnim(enterAnim)
        cancelAnim(textAlphaAnim)
        cancelAnimSet(textShowAnimSet)
        cancelAnimSet(cmdExecutedAnimSet)
    }

    private fun cancelAnim(anim: Animator?) {
        anim?.cancel()
        anim?.removeAllListeners()
        if (anim != null && anim is ValueAnimator) {
            anim.removeAllUpdateListeners()
        }
    }

    private fun cancelAnimSet(animSet: AnimatorSet?) {
        animSet?.cancel()
        animSet?.removeAllListeners()
        animSet?.childAnimations?.forEach {
            cancelAnim(it)
        }
    }

    /**
     * 隐藏动画
     */
    private fun doExitAnim() {
        exitAnim = ValueAnimator.ofFloat(this.alpha, 0f)
        exitAnim!!.duration = 300
        exitAnim!!.interpolator = PathInterpolator(0.33f, 0f, 0.66f, 1f)
        exitAnim!!.addUpdateListener {
            this.alpha = it.animatedValue as Float
            if (!isEnableReduceAnimFrameRate) {
                this@SpeechASRAnimTextView.invalidate()
            }
        }
        exitAnim!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                reset()
                if (canShowText && textDrawTaskList.isNotEmpty()) {
                    this@SpeechASRAnimTextView.visibility = VISIBLE
                    val task = textDrawTaskList.first()
                    textDrawTaskList.clear()
                    setText(task.text)
                } else {
                    textDrawTaskList.clear()
                    post {
                        hideFinishCallback.invoke()
                        needChangeTextColorOrPos = false
                        releaseMem()
                        if (isEnableReduceAnimFrameRate) {
                            unregisterViewRefreshCallback()
                        }
                    }
                }
                Log.i(TAG, "exitAnim: onAnimationEnd")
            }
        })
        exitAnim!!.start()
    }

    /**
     * 获取text宽度
     *
     */
    private fun getTextWidth(text: String): Float {
        var width = textPaint.measureText(text)
        Log.i(TAG, "getTextWidth  $text  --> $width ")
        return width
    }

    /**
     * 设置自定义宽高的方法
     */
    private fun setCustomSize(width: Float, height: Float) {
        customWidth = width.toInt()
        customHeight = height.toInt()
        requestLayout() // 请求重新布局
    }



    fun releaseMem() {
        textPaint.shader = null
        textShader = null
    }

    private fun registerViewRefreshCallback() {
        refreshViewController.addViewRefreshCallback(viewRefreshCallback)
    }

    private fun unregisterViewRefreshCallback() {
        refreshViewController.removeViewRefreshCallback(viewRefreshCallback)
        refreshViewController.release()
    }

    private fun isAnimRunning(): Boolean {
        return (enterAnim != null && enterAnim!!.isRunning) ||
                (exitAnim != null && exitAnim!!.isRunning) ||
                (textAlphaAnim != null && textAlphaAnim!!.isRunning) ||
                (textShowAnimSet != null && textShowAnimSet!!.isRunning)
    }

    inner class TextDrawTask {
        val text: String
        val textWidth: Float
        var textAlpha: Int = 0

        constructor(text: String) {
            this.text = text
            this.textWidth = getTextWidth(text)
        }
    }

    inner class SetCmdExecutedStateTask {
        val input: String
        var recordId: String = ""

        constructor(input: String, recordId: String) {
            this.input = input
            this.recordId = recordId
        }

        override fun toString(): String {
            return "input: $input, recordId: $recordId"
        }
    }

    enum class TextState {
        NONE,
        CONTAINS, //持续上屏
        OTHER, //其他文本
        CMD_EXECUTED, //文本命令已执行状态
    }
    /**
     * 代码中修改 asrTextColor 的对外方法
     * @param color 目标文本颜色（支持 Color.parseColor("#FFFFFF") 或资源色 R.color.xxx）
     */
    fun setAsrTextColor(color: Int) {
        // 1. 更新文本颜色变量
        this.textColor = color
        // 2. 更新文本 Paint 的颜色（确保新颜色生效）
        textPaint.color = textColor
        // 3. 重置文本 shader（避免旧 shader 残留导致颜色异常）
        textShader = null
        // 4. 标记需要重新计算颜色/位置，确保重绘时使用新颜色
        needChangeTextColorOrPos = true
        // 5. 触发视图重绘（关键：让新颜色显示在屏幕上）
        invalidate()
        AILog.i(TAG, "asrTextColor updated to: $color")
    }

    /**
     * 重载方法：支持通过资源 ID 设置颜色（如 R.color.style2_color_2）
     */
    fun setAsrTextColorRes(colorResId: Int) {
        try {
            // 从资源中获取颜色值（需传入 Context，控件自身的 context 已初始化）
            val color = context.resources.getColor(colorResId, context.theme)
            setAsrTextColor(color)
        } catch (e: Resources.NotFoundException) {
            AILog.e(TAG, "Color resource not found: $colorResId")
        }
    }
}