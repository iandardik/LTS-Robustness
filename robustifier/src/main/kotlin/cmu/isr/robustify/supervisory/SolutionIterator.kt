package cmu.isr.robustify.supervisory

import cmu.isr.lts.CompactDetLTS
import org.slf4j.LoggerFactory

class SolutionIterator(

) : Iterable<CompactDetLTS<String>> {

  private val logger = LoggerFactory.getLogger(javaClass)

  override fun iterator(): Iterator<CompactDetLTS<String>> {
    TODO("Not yet implemented")
  }

}