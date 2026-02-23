package org.ukky.suggestfulltext

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SuggestFullTextApplication

fun main(args: Array<String>) {
    runApplication<SuggestFullTextApplication>(*args)
}
