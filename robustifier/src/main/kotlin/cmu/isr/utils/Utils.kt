package cmu.isr.utils

import java.time.Duration

fun Duration.pretty(): String {
  return "%02d:%02d:%02d:%03d".format(toHours(), toMinutes() % 60, seconds % 60, toMillis() % 1000)
}