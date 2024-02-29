package com.tanasi.streamflix.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.tanasi.streamflix.R
import com.tanasi.streamflix.adapters.AppAdapter
import com.tanasi.streamflix.database.AppDatabase
import com.tanasi.streamflix.databinding.DialogShowOptionsBinding
import com.tanasi.streamflix.fragments.home.HomeFragment
import com.tanasi.streamflix.fragments.home.HomeFragmentDirections
import com.tanasi.streamflix.fragments.season.SeasonFragment
import com.tanasi.streamflix.models.Episode
import com.tanasi.streamflix.models.Movie
import com.tanasi.streamflix.models.TvShow
import com.tanasi.streamflix.utils.WatchNextUtils
import com.tanasi.streamflix.utils.getCurrentFragment
import com.tanasi.streamflix.utils.toActivity

@SuppressLint("RestrictedApi")
class ShowOptionsDialog(context: Context) : Dialog(context) {

    private val binding = DialogShowOptionsBinding.inflate(LayoutInflater.from(context))

    private val database = AppDatabase.getInstance(context)

    var show: AppAdapter.Item? = null
        set(value) {
            when (value) {
                is Episode -> displayEpisode(value)
                is Movie -> displayMovie(value)
                is TvShow -> displayTvShow(value)
            }
            field = value
        }

    init {
        setContentView(binding.root)

        binding.btnOptionCancel.setOnClickListener {
            hide()
        }


        window?.attributes = window?.attributes?.also { param ->
            param.gravity = Gravity.END
        }
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.35).toInt(),
            context.resources.displayMetrics.heightPixels
        )
    }


    private fun displayEpisode(episode: Episode) {
        val saved = database.episodeDao().getEpisode(episode.id)?.also {
            episode.isWatched = it.isWatched
        }
        val program = WatchNextUtils.getProgram(context, episode.id)

        Glide.with(context)
            .load(episode.poster ?: episode.tvShow?.poster)
            .fitCenter()
            .into(binding.ivOptionsShowPoster)

        binding.tvOptionsShowTitle.text = episode.title

        binding.tvShowSubtitle.text = episode.season?.number?.let { seasonNumber ->
            context.getString(
                R.string.episode_item_info,
                seasonNumber,
                episode.number,
                episode.title,
            )
        } ?: context.getString(
            R.string.episode_item_info_episode_only,
            episode.number,
            episode.title,
        )


        binding.btnOptionEpisodeOpenTvShow.apply {
            setOnClickListener {
                when (val fragment = context.toActivity()?.getCurrentFragment()) {
                    is HomeFragment -> episode.tvShow?.let { tvShow ->
                        NavHostFragment.findNavController(fragment).navigate(
                            HomeFragmentDirections.actionHomeToTvShow(
                                id = tvShow.id
                            )
                        )
                    }
                }
                hide()
            }

            visibility = when (context.toActivity()?.getCurrentFragment()) {
                is HomeFragment -> View.VISIBLE
                else -> View.GONE
            }
        }

        binding.btnOptionShowFavorite.visibility = View.GONE

        binding.btnOptionShowWatched.apply {
            setOnClickListener {
                saved?.let {
                    database.episodeDao().updateWatched(
                        id = episode.id,
                        isWatched = !episode.isWatched
                    )
                    episode.isWatched = !episode.isWatched
                } ?: let { _ ->
                    episode.isWatched = !episode.isWatched
                    database.episodeDao().insert(episode)
                }
                if (episode.isWatched) {
                    program?.let { WatchNextUtils.deleteProgramById(context, program.id) }
                }

                when (val fragment = context.toActivity()?.getCurrentFragment()) {
                    is HomeFragment -> fragment.refresh()
                    is SeasonFragment -> fragment.refresh(episode)
                }
                hide()
            }

            text = when {
                episode.isWatched -> context.getString(R.string.option_show_unwatched)
                else -> context.getString(R.string.option_show_watched)
            }
            visibility = View.VISIBLE
        }

        binding.btnOptionProgramClear.apply {
            setOnClickListener {
                if (program == null) return@setOnClickListener

                WatchNextUtils.deleteProgramById(context, program.id)
                when (val fragment = context.toActivity()?.getCurrentFragment()) {
                    is HomeFragment -> fragment.refresh()
                    is SeasonFragment -> fragment.refresh(episode)
                }
                hide()
            }

            visibility = when {
                program != null -> View.VISIBLE
                else -> View.GONE
            }
        }
    }

    private fun displayMovie(movie: Movie) {
    }

    private fun displayTvShow(tvShow: TvShow) {
    }
}