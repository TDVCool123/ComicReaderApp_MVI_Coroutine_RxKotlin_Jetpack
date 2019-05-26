package com.hoc.comicapp.data

import com.hoc.comicapp.CoroutinesDispatcherProvider
import com.hoc.comicapp.data.models.Chapter
import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.data.models.ComicAppError
import com.hoc.comicapp.utils.Either
import com.hoc.comicapp.utils.right
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import kotlin.random.Random
import kotlin.random.nextInt

class MockComicRepositoryImpl(private val dispatcherProvider: CoroutinesDispatcherProvider) :
  ComicRepository {
  override suspend fun getComicDetail(comicLink: String): Either<ComicAppError, Comic> {
    TODO()
  }

  override suspend fun getTopMonth(): Either<ComicAppError, List<Comic>> {
    delay(2_000L)

    return withContext(dispatcherProvider.io) {
      val decimalFormat = DecimalFormat("###,###")
      List(10) {
        Comic(
          title = "Test title $it",
          view = decimalFormat.format(Random.nextInt(100_000..2_000_000)),
          thumbnail = "",
          chapters = listOf(
            Chapter(
              chapterName = "Chapter 1",
              chapterLink = "",
              time = null,
              view = null,
              images = null
            )
          ),
          link = "",
          moreDetail = null
        )
      }.right()
    }
  }

  override suspend fun getUpdate(page: Int?): Either<ComicAppError, List<Comic>> {
    TODO()
  }

  override suspend fun getSuggest(): Either<ComicAppError, List<Comic>> {
    delay(2_000L)

    return withContext(dispatcherProvider.io) {
      val decimalFormat = DecimalFormat("###,###")
      List(10) {
        Comic(
          title = "Test title $it",
          view = decimalFormat.format(Random.nextInt(100_000..2_000_000)),
          thumbnail = "",
          chapters = listOf(
            Chapter(
              chapterName = "Chapter 1",
              chapterLink = "",
              time = "$it giờ trước",
              view = null,
              images = null
            )
          ),
          link = "",
          moreDetail = null
        )
      }.right()
    }
  }
}