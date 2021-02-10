/*
 * Copyright (c) 2021 Proton Technologies AG
 *
 * This file is part of ProtonVPN.
 *
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.protonvpn.android.tv.detailed

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.PresenterSelector
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.Observer
import com.protonvpn.android.R
import com.protonvpn.android.components.BaseTvBrowseFragment
import com.protonvpn.android.tv.TvUpgradeActivity
import com.protonvpn.android.tv.detailed.TvServerListScreenFragment.Companion.EXTRA_COUNTRY
import com.protonvpn.android.tv.presenters.AbstractCardPresenter
import com.protonvpn.android.utils.AndroidUtils.launchActivity

class TvServerListFragment : BaseTvBrowseFragment() {

    private val viewModel by viewModels<TvServerListViewModel> { viewModelFactory }
    private var rowsAdapter: ArrayObjectAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(requireArguments()[EXTRA_COUNTRY] as String)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rowsAdapter = ArrayObjectAdapter(ServerListRowPresenter())
        adapter = rowsAdapter

        viewModel.servers.observe(viewLifecycleOwner, Observer {
            updateServerList(it)
        })

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            require(item is TvServerListViewModel.ServerViewModel)
            item.click(requireContext()) {
                requireContext().launchActivity<TvUpgradeActivity>()
            }
        }

        startEntranceTransition()
    }

    override fun onDestroyView() {
        rowsAdapter = null
        super.onDestroyView()
    }

    private fun updateServerList(viewModel: TvServerListViewModel.ServersViewModel) {
        viewModel.servers.onEachIndexed { i, (group, servers) ->
            rowsAdapter?.add(i, createRow(group, servers, i))
        }
    }

    private fun createRow(
        group: TvServerListViewModel.ServerGroup,
        servers: List<TvServerListViewModel.ServerViewModel>,
        index: Int
    ): Row {
        val listRowAdapter = ArrayObjectAdapter(ServersPresenterSelector(requireContext()))
        for (server in servers)
            listRowAdapter.add(server)
        return ServersListRow(HeaderItem(group.toLabel(servers.size)), listRowAdapter, index)
    }

    private fun TvServerListViewModel.ServerGroup.toLabel(count: Int) = when (this) {
        TvServerListViewModel.ServerGroup.Recents -> getString(R.string.tv_recently_used_servers, count)
        TvServerListViewModel.ServerGroup.Available -> getString(R.string.tv_available_servers, count)
        TvServerListViewModel.ServerGroup.Locked -> getString(R.string.tv_locked_servers, count)
        TvServerListViewModel.ServerGroup.Other -> getString(R.string.tv_other_servers, count)
        is TvServerListViewModel.ServerGroup.City -> "$name ($count)"
    }

    private inner class ServerListRowPresenter : FadeListRowPresenter(false) {
        override fun rowAlpha(index: Int, selectedIdx: Int) = when {
            index < selectedIdx - 1 -> 0f
            index == selectedIdx - 1 -> BASE_INACTIVE_ROW_ALPHA
            index > selectedIdx -> BASE_INACTIVE_ROW_ALPHA / (index - selectedIdx)
            else -> 1f
        }

        override fun RowPresenter.ViewHolder.getRowIndex() =
            (rowObject as ServersListRow).index
    }

    inner class ServersPresenterSelector(context: Context) : PresenterSelector() {
        private val presenter = ServerPresenter(context)
        override fun getPresenter(item: Any): Presenter = presenter
    }

    class ServersListRow(header: HeaderItem?, adapter: ObjectAdapter, val index: Int) : ListRow(header, adapter)

    inner class ServerPresenter(context: Context) :
        AbstractCardPresenter<TvServerListViewModel.ServerViewModel, TvServerCardView>(context) {

        override fun onCreateView() = TvServerCardView(context, viewLifecycleOwner)

        override fun onBindViewHolder(card: TvServerListViewModel.ServerViewModel, cardView: TvServerCardView) {
            cardView.bind(card)
        }

        override fun onUnbindViewHolder(cardView: TvServerCardView) {
            cardView.unbind()
        }
    }

    companion object {
        private const val BASE_INACTIVE_ROW_ALPHA = 0.6f
    }
}
