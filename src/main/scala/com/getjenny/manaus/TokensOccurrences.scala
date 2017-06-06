package com.getjenny.manaus

abstract class TokensOccurrences() {
  def getTokensOccurrencesMap(): Map[String, Int]
  def getOccurrence(word: String): Int
  def getTokenN(): Int
}
