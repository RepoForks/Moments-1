/*
 * Moments - To the best Instagram client
 * Copyright (C) 2015  XiNGRZ <xxx@oxo.ooo>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program;  if not, see <http://www.gnu.org/licenses/>.
 */

package ooo.oxo.moments.feed;

import android.content.Intent;
import android.databinding.ObservableArrayList;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jakewharton.rxbinding.support.v4.widget.RxSwipeRefreshLayout;
import com.trello.rxlifecycle.components.support.RxFragment;

import java.util.List;

import butterknife.ButterKnife;
import ooo.oxo.moments.InstaApplication;
import ooo.oxo.moments.MainActivity;
import ooo.oxo.moments.R;
import ooo.oxo.moments.ViewerActivity;
import ooo.oxo.moments.api.FeedApi;
import ooo.oxo.moments.databinding.FeedFragmentBinding;
import ooo.oxo.moments.model.Media;
import ooo.oxo.moments.rx.RxEndlessRecyclerView;
import ooo.oxo.moments.rx.RxList;
import ooo.oxo.moments.user.UserActivity;
import ooo.oxo.moments.util.ImageCandidatesUtil;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class FeedFragment extends RxFragment implements
        FeedAdapter.FeedListener {

    private static final String TAG = "FeedFragment";

    private final ObservableArrayList<Media> feed = new ObservableArrayList<>();

    private FeedFragmentBinding binding;

    private FeedApi feedApi;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        feedApi = InstaApplication.from(getContext()).createApi(FeedApi.class);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FeedFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ButterKnife.bind(this, view);

        binding.appbar.addOnOffsetChangedListener((v, i) -> binding.statusBar.setAlpha(Math.min(
                1, (float) -i / (float) (binding.appbar.getHeight() - binding.statusBar.getHeight()))));

        binding.toolbar.setNavigationOnClickListener(v -> ((MainActivity) getActivity()).openNavigation());

        binding.refresher.setColorSchemeResources(R.color.primary);

        binding.content.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.content.setAdapter(new FeedAdapter(getContext(), feed, this));

        binding.setFeed(feed);

        RxEndlessRecyclerView.reachesEnd(binding.content)
                .compose(bindToLifecycle())
                .map(feed::get)
                .flatMap(last -> load(last.id))
                .subscribe(RxList.appendTo(feed), this::showError);

        RxSwipeRefreshLayout.refreshes(binding.refresher)
                .compose(bindToLifecycle())
                .flatMap(avoid -> load(null))
                .subscribe(RxList.replace(feed), this::showError);

        load(null)
                .compose(bindToLifecycle())
                .subscribe(RxList.replace(feed), this::showError);

        binding.refresher.post(() -> binding.refresher.setRefreshing(true));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        feed.clear();
    }

    private Observable<List<Media>> load(String maxId) {
        return feedApi.timeline(maxId)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(() -> binding.refresher.setRefreshing(true))
                .doOnCompleted(() -> binding.refresher.setRefreshing(false))
                .filter(envelope -> envelope.items != null)
                .map(envelope -> envelope.items);
    }

    private void showError(Throwable error) {
        Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUserClick(FeedAdapter.ViewHolder holder) {
        Media item = feed.get(holder.getAdapterPosition());

        Intent intent = new Intent(getContext(), UserActivity.class);
        intent.putExtra("user", item.user);
        intent.putExtra("from_post", item.id);

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                getActivity(), holder.binding.avatar, item.id + "_avatar");

        getActivity().startActivity(intent, options.toBundle());
    }

    @Override
    public void onUserClick(long id) {
        Intent intent = new Intent(getContext(), UserActivity.class);
        intent.putExtra("id", id);
        startActivity(intent);
    }

    @Override
    public void onImageClick(FeedAdapter.ViewHolder holder) {
        Media item = feed.get(holder.getAdapterPosition());
        Media.Resource best = ImageCandidatesUtil.pickBest(item.imageVersions.candidates);

        if (best == null) {
            return;
        }

        Intent intent = new Intent(getContext(), ViewerActivity.class);
        intent.setData(Uri.parse(best.url));

        if (item.imageVersions.picked != null) {
            intent.putExtra("thumbnail", item.imageVersions.picked.url);
        }

        if (item.caption != null) {
            intent.putExtra("caption", item.caption.text);
        }

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                getActivity(), holder.binding.image, best.url);

        getActivity().startActivity(intent, options.toBundle());
    }

    @Override
    public void onLikesClick(FeedAdapter.ViewHolder holder) {
    }

    @Override
    public void onLike(FeedAdapter.ViewHolder holder) {
    }

    @Override
    public void onComment(FeedAdapter.ViewHolder holder) {
    }

}
