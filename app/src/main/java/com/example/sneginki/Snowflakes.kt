package com.example.sneginki

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.AsyncTask
import android.view.MotionEvent
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

// Структура данных для одной снежинки
data class Snowflake(
    var x: Float,
    var y: Float,
    val velocity: Float,
    val radius: Float,
    val color: Int,
    var phase: Float // Начальная фаза для функции синуса (горизонтальное блуждание)
)

lateinit var snow: Array<Snowflake>
val paint = Paint()
var h = 1000
var w = 1000

open class Snowflakes(ctx: Context) : View(ctx) {
    private var moveTask: MoveTask? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Заливаем фон темно-синим цветом ночного неба
        canvas.drawColor(Color.rgb(10, 15, 45))

        for (s in snow) {
            paint.color = s.color
            canvas.drawCircle(s.x, s.y, s.radius, paint)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        h = bottom - top
        w = right - left

        val r = Random(System.currentTimeMillis())

        // Генерация массива снежинок
        snow = Array(150) {
            // Генерируем оттенки от голубого до белого
            val red = r.nextInt(160, 256)
            val green = r.nextInt(190, 256)
            val blue = 255 // Синий канал на максимуме для холодного оттенка

            Snowflake(
                x = r.nextFloat() * w,
                y = r.nextFloat() * h,
                velocity = 6f + 12f * r.nextFloat(), // Базовая скорость
                radius = 8f + 16f * r.nextFloat(),   // Размер снежинки
                color = Color.rgb(red, green, blue),
                phase = r.nextFloat() * 6.28f        // Случайная фаза от 0 до 2*PI
            )
        }
    }

    fun moveSnowflakes() {
        for (s in snow) {
            // Движение с замедлением:
            // Наверху (y = 0) множитель равен 1.0 (максимальная скорость).
            // Внизу (y = h) множитель снижается до 0.25 (замедление в 4 раза).
            val slowdownFactor = 1.0f - 0.75f * (s.y / h.toFloat()).coerceIn(0f, 1f)
            s.y += s.velocity * slowdownFactor

            // Блуждания по горизонтали
            // Амплитуда качания зависит от изменения координаты y и индивидуальной фазы
            s.x += sin(s.y / 35f + s.phase) * 3.5f

            // Если снежинка упала за нижнюю границу, возвращаем её наверх
            if (s.y > h + s.radius) {
                s.y = -s.radius
                s.x = Random.nextFloat() * w
            }

            // Зацикливание по горизонтали, если снежинку унесло ветром за край
            if (s.x > w + s.radius) s.x = -s.radius
            if (s.x < -s.radius) s.x = w + s.radius
        }

        // Запрашиваем перерисовку экрана в UI-потоке
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Запуск фонового потока по нажатию на экран (чтобы не запускать несколько раз подряд)
        if (event?.action == MotionEvent.ACTION_DOWN) {
            if (moveTask == null || moveTask?.status == AsyncTask.Status.FINISHED) {
                moveTask = MoveTask(this)
                moveTask?.execute()
            }
        }
        return true
    }

    // Внутренний класс AsyncTask для выполнения анимации в фоновом потоке
    @Suppress("DEPRECATION")
    class MoveTask(val s: Snowflakes) : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) {
            while (!isCancelled) {
                try {
                    Thread.sleep(25) // Задержка +-40 кадров в секунду
                    publishProgress() // Переход к вызову onProgressUpdate в UI-потоке
                } catch (e: InterruptedException) {
                    break
                }
            }
        }

        override fun onProgressUpdate(vararg values: Unit?) {
            super.onProgressUpdate(*values)
            // Пересчет координат и вызов invalidate() безопасно выполняются в главном потоке
            s.moveSnowflakes()
        }
    }
}