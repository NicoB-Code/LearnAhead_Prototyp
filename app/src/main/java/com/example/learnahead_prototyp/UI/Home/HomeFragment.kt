package com.example.learnahead_prototyp.UI.Home

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.learnahead_prototyp.Data.Model.LearningCategory
import com.example.learnahead_prototyp.Data.Model.User
import com.example.learnahead_prototyp.R
import com.example.learnahead_prototyp.UI.Auth.AuthViewModel
import com.example.learnahead_prototyp.UI.LearningCategory.LearnCategoryViewModel
import com.example.learnahead_prototyp.Util.UiState
import com.example.learnahead_prototyp.Util.hide
import com.example.learnahead_prototyp.Util.show
import com.example.learnahead_prototyp.Util.toast
import com.example.learnahead_prototyp.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Ein Fragment für den Home-Bildschirm der App.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val learningCategoryViewModel: LearnCategoryViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private val adapter by lazy {
        HomeAdapter(this::onItemClicked)
    }
    var pointsBefore = 0
    private var currentUser: User? = null
    private var learningCategoryList: MutableList<LearningCategory> = mutableListOf()

    /**
     * Erstellt die View des Fragments.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setLocalCurrentUser()
        setupEventListeners()
        currentUser?.let { authViewModel.updateUserInfo(it) }
    }

    /**
     * Ruft die aktuelle Benutzersitzung ab.
     */
    private fun setLocalCurrentUser() {
        authViewModel.getSession()
    }

    /**
     * Setzt die Event-Listener für die Buttons und RecyclerView.
     */
    private fun setupEventListeners() {
        binding.todaysLearningGoalsRecyclerView.adapter = adapter

        binding.learningCategoriesButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_learningCategoryListFragment)
        }

        binding.learningGoalsButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_goalListingFragment)
        }

        binding.logoutIcon.setOnClickListener {
            authViewModel.logout {
                findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
            }
        }

        binding.profileIcon.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }
    }

    /**
     * Setzt die Observer für die Datenaktualisierungen.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        learningCategoryViewModel.learningCategories.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.show()
                }

                is UiState.Failure -> {
                    binding.progressBar.hide()
                    toast(state.error)
                }

                is UiState.Success -> {
                    binding.progressBar.hide()
                    learningCategoryList = state.data.toMutableList()
                    val today = LocalDate.now()

                    val filteredList = state.data.filter { learningCategory ->
                        val goal = learningCategory.relatedLearningGoal
                        val startDate = goal?.startDate?.toInstant()?.atZone(ZoneId.systemDefault())
                            ?.toLocalDate()
                        val endDate = goal?.endDate?.toInstant()?.atZone(ZoneId.systemDefault())
                            ?.toLocalDate()

                        if (startDate != null && endDate != null && today >= startDate) {
                            val daysBetweenDates = ChronoUnit.DAYS.between(startDate, endDate)
                            val daysSinceStart = ChronoUnit.DAYS.between(startDate, today)

                            val interval = if (daysBetweenDates < 10) {
                                1 // Set interval to 1 if daysBetweenDates is less than 10
                            } else {
                                ((daysBetweenDates.toDouble() + 1) / 10).toInt() // Calculate the interval
                            }

                            val learningDayIndex = if (interval > 0) {
                                (daysSinceStart / interval) + 1
                            } else {
                                0
                            }

                            val nextLearningDay = startDate.plusDays((learningDayIndex - 1) * interval)

                            today == nextLearningDay && today != startDate
                        } else {
                            false
                        }
                    }.toMutableList()

                    adapter.updateList(filteredList)
                }
            }
        }

        authViewModel.currentUser.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.show()
                }

                is UiState.Failure -> {
                    binding.progressBar.hide()
                    toast(state.error)
                }

                is UiState.Success -> {
                    binding.progressBar.hide()
                    currentUser = state.data
                    pointsBefore = currentUser?.currentPoints!!
                    learningCategoryViewModel.getLearningCategories(currentUser)
                }
            }
        }

        authViewModel.updateUserInfo.observe(viewLifecycleOwner) { state ->
            binding.progressBar.visibility = when (state) {
                is UiState.Loading -> View.VISIBLE
                is UiState.Failure -> {
                    toast(state.error)
                    View.GONE
                }
                is UiState.Success -> {
                    var pointsNow =currentUser?.currentPoints!!
                    if(pointsBefore != pointsNow ){
                        var changedPoints = pointsNow- pointsBefore
                        toast("Du hast gerade $changedPoints Punkte für deinen Login erhalten!")
                    }
                    View.GONE
                }
            }
        }
    }

    /**
     * Handler für den Klick auf ein Item in der RecyclerView.
     */
    private fun onItemClicked(position: Int, item: LearningCategory) {
        findNavController().navigate(
            R.id.action_homeFragment_to_learningCategoryInnerViewFragment,
            Bundle().apply {
                learningCategoryViewModel.setCurrentSelectedLearningCategory(item)
                putParcelable("learning_category", item)
            }
        )
    }
}
