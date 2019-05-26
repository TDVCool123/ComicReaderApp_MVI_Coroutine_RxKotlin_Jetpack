package com.hoc.comicapp.ui.home

import androidx.lifecycle.viewModelScope
import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.data.models.getMessageFromError
import com.hoc.comicapp.utils.Event
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.notOfType
import com.hoc.comicapp.utils.setValueIfNew
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber

@ExperimentalCoroutinesApi
class HomeViewModel(private val homeInteractor: HomeInteractor) :
  BaseViewModel<HomeViewIntent, HomeViewState, HomeSingleEvent>() {
  override val initialState = HomeViewState.initialState()

  private val intentS = PublishRelay.create<HomeViewIntent>()
  private val stateS = BehaviorRelay.createDefault<HomeViewState>(initialState)
  private val compositeDisposable = CompositeDisposable()

  /**
   * Transform [HomeViewIntent.Initial]s to [HomePartialChange]s
   */
  private val initialProcessor =
    ObservableTransformer<HomeViewIntent.Initial, HomePartialChange> { intent ->
      intent
        .flatMap {
          Observable.mergeArray(
            homeInteractor
              .suggestComicsPartialChanges(coroutineScope = viewModelScope)
              .doOnNext {
                val messageFromError = (it as? HomePartialChange.SuggestHomePartialChange.Error)
                  ?.error
                  ?.let(::getMessageFromError)
                  ?: return@doOnNext
                sendMessageEvent("Get suggest list error: $messageFromError")
              },
            homeInteractor
              .topMonthComicsPartialChanges(coroutineScope = viewModelScope)
              .doOnNext {
                val messageFromError = (it as? HomePartialChange.TopMonthHomePartialChange.Error)
                  ?.error
                  ?.let(::getMessageFromError)
                  ?: return@doOnNext
                sendMessageEvent("Get top month list error: $messageFromError")
              },
            homeInteractor
              .updatedComicsPartialChanges(page = 1, coroutineScope = viewModelScope)
              .doOnNext {
                val messageFromError = (it as? HomePartialChange.UpdatedPartialChange.Error)
                  ?.error
                  ?.let(::getMessageFromError)
                  ?: return@doOnNext
                sendMessageEvent("Get updated list error: $messageFromError")
              }
          )
        }
    }

  /**
   * Transform [HomeViewIntent.Refresh]s to [HomePartialChange]s
   */
  private val refreshProcessor =
    ObservableTransformer<HomeViewIntent.Refresh, HomePartialChange> { intent ->
      intent
        .exhaustMap {
          homeInteractor
            .refreshAllPartialChanges(coroutineScope = viewModelScope)
            .doOnNext {
              sendMessageEvent(
                when (it) {
                  is HomePartialChange.RefreshPartialChange.RefreshSuccess -> "Refresh successfully"
                  is HomePartialChange.RefreshPartialChange.RefreshFailure -> "Refresh not successfully"
                  else -> return@doOnNext
                }
              )
            }
        }
    }

  /**
   * Transform [HomeViewIntent.LoadNextPageUpdatedComic]s to [HomePartialChange]s
   */
  private val loadNextPageProcessor =
    ObservableTransformer<HomeViewIntent.LoadNextPageUpdatedComic, HomePartialChange> { intent ->
      intent
        .withLatestFrom(stateS)
        .filter {
          !it.second
            .items
            .any(HomeListItem::isLoadingOrError)
        }
        .map { it.second.updatedPage + 1 }
        .doOnNext { Timber.d("[~~~] load_next_page = $it") }
        .exhaustMap {
          homeInteractor.updatedComicsPartialChanges(
            page = it,
            coroutineScope = viewModelScope
          )
        }
    }

  /**
   * Transform [HomeViewIntent.RetryUpdate]s to [HomePartialChange]s
   */
  private val retryUpdateProcessor =
    ObservableTransformer<HomeViewIntent.RetryUpdate, HomePartialChange> { intent ->
      intent
        .withLatestFrom(stateS)
        .map { it.second.updatedPage + 1 }
        .doOnNext { Timber.d("[~~~] refresh_page=$it") }
        .exhaustMap {
          homeInteractor
            .updatedComicsPartialChanges(page = it, coroutineScope = viewModelScope)
            .doOnNext {
              val messageFromError = (it as? HomePartialChange.UpdatedPartialChange.Error)
                ?.error
                ?.let(::getMessageFromError)
                ?: return@doOnNext
              sendMessageEvent("Error when retry get updated list: $messageFromError")
            }
        }
    }

  /**
   * Transform [HomeViewIntent.RetrySuggest]s to [HomePartialChange]s
   */
  private val retrySuggestProcessor =
    ObservableTransformer<HomeViewIntent.RetrySuggest, HomePartialChange> {
      it.exhaustMap {
        homeInteractor
          .suggestComicsPartialChanges(coroutineScope = viewModelScope)
          .doOnNext {
            val messageFromError = (it as? HomePartialChange.SuggestHomePartialChange.Error)
              ?.error
              ?.let(::getMessageFromError)
              ?: return@doOnNext
            sendMessageEvent("Error when retry get suggest list: $messageFromError")
          }
      }
    }

  /**
   * Transform [HomeViewIntent.RetryTopMonth]s to [HomePartialChange]s
   */
  private val retryTopMonthProcessor =
    ObservableTransformer<HomeViewIntent.RetryTopMonth, HomePartialChange> {
      it.exhaustMap {
        homeInteractor
          .topMonthComicsPartialChanges(coroutineScope = viewModelScope)
          .doOnNext {
            val messageFromError = (it as? HomePartialChange.TopMonthHomePartialChange.Error)
              ?.error
              ?.let(::getMessageFromError)
              ?: return@doOnNext
            sendMessageEvent("Error when retry get top month list: $messageFromError")
          }
      }
    }

  /**
   * Filters intent by type, then compose with [ObservableTransformer] to transform [HomeViewIntent] to [HomePartialChange].
   * Then using [Observable.scan] operator with reducer to transform [HomePartialChange] [HomeViewState]
   */
  private val intentToViewState = ObservableTransformer<HomeViewIntent, HomeViewState> {
    it.publish { shared ->
      Observable.mergeArray(
        shared
          .ofType<HomeViewIntent.Initial>()
          .compose(initialProcessor),
        shared
          .ofType<HomeViewIntent.Refresh>()
          .compose(refreshProcessor),
        shared
          .ofType<HomeViewIntent.LoadNextPageUpdatedComic>()
          .compose(loadNextPageProcessor),
        shared
          .ofType<HomeViewIntent.RetryUpdate>()
          .compose(retryUpdateProcessor),
        shared
          .ofType<HomeViewIntent.RetrySuggest>()
          .compose(retrySuggestProcessor),
        shared
          .ofType<HomeViewIntent.RetryTopMonth>()
          .compose(retryTopMonthProcessor)
      )
    }.doOnNext { Timber.d("partial_change=$it") }
      .scan(HomeViewState.initialState()) { state, change -> change.reducer(state) }
      .distinctUntilChanged()
      .observeOn(AndroidSchedulers.mainThread())
  }

  override fun processIntents(intents: Observable<HomeViewIntent>) =
    intents.subscribe(intentS::accept)!!

  init {
    intentS
      .compose(intentFilter)
      .doOnNext { Timber.d("intent=$it") }
      .compose(intentToViewState)
      .doOnNext { Timber.d("view_state=$it") }
      .subscribeBy(onNext = stateS::accept)
      .addTo(compositeDisposable)

    stateS
      .subscribeBy(onNext = stateD::setValueIfNew)
      .addTo(compositeDisposable)
  }

  override fun onCleared() {
    super.onCleared()
    compositeDisposable.clear()
    Timber.d("onCleared")
  }

  private fun sendMessageEvent(message: String) {
    singleEventD.value = Event(HomeSingleEvent.MessageEvent(message))
  }

  private companion object {
    /**
     * Only take 1 [HomeViewIntent.Initial]
     */
    @JvmStatic
    private val intentFilter = ObservableTransformer<HomeViewIntent, HomeViewIntent> {
      it.publish { shared ->
        Observable.mergeArray(
          shared
            .ofType<HomeViewIntent.Initial>()
            .take(1),
          shared.notOfType<HomeViewIntent.Initial, HomeViewIntent>()
        )
      }
    }
  }
}