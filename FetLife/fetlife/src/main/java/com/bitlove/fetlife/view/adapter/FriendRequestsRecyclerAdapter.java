package com.bitlove.fetlife.view.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bitlove.fetlife.FetLifeApplication;
import com.bitlove.fetlife.R;
import com.bitlove.fetlife.model.pojos.FriendRequest;
import com.bitlove.fetlife.model.pojos.FriendRequest_Table;
import com.bitlove.fetlife.model.pojos.FriendSuggestion;
import com.bitlove.fetlife.model.pojos.FriendSuggestion_Table;
import com.bitlove.fetlife.model.resource.ImageLoader;
import com.bitlove.fetlife.model.service.FetLifeApiIntentService;
import com.bitlove.fetlife.view.activity.ResourceListActivity;
import com.crashlytics.android.Crashlytics;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FriendRequestsRecyclerAdapter extends RecyclerView.Adapter<FriendRequestScreenViewHolder> {

    private static final int FRIENDREQUEST_UNDO_DURATION = 5000;
    private static final int VIEWTYPE_HEADER = 0;
    private static final int VIEWTYPE_ITEM = 1;

    public interface OnFriendRequestClickListener {
        public void onItemClick(FriendRequest friendRequest);
        public void onAvatarClick(FriendRequest friendRequest);
    }

    public interface OnFriendSuggestionClickListener {
        public void onItemClick(FriendSuggestion friendSuggestion);
        public void onAvatarClick(FriendSuggestion friendSuggestion);
    }

    static class Undo {
        AtomicBoolean pending = new AtomicBoolean(true);
    }

    private final ImageLoader imageLoader;

    private List<FriendRequest> friendRequestList;
    private List<FriendSuggestion> friendSuggestionList;
    OnFriendRequestClickListener onFriendRequestClickListener;
    OnFriendSuggestionClickListener onFriendSuggestionClickListener;

    public FriendRequestsRecyclerAdapter(ImageLoader imageLoader, boolean clearItems) {
        this.imageLoader = imageLoader;
        if (clearItems) {
            clearItems();
        } else {
            loadItems();
        }
    }

    public void setOnFriendRequestClickListener(OnFriendRequestClickListener onFriendRequestClickListener) {
        this.onFriendRequestClickListener = onFriendRequestClickListener;
    }

    public void setOnFriendSuggestionClickListener(OnFriendSuggestionClickListener onFriendSuggestionClickListener) {
        this.onFriendSuggestionClickListener = onFriendSuggestionClickListener;
    }

    private void loadItems() {
        //TODO: think of moving to separate thread with specific DB executor
        try {
            friendRequestList = new Select().from(FriendRequest.class).where(FriendRequest_Table.pending.is(false)).queryList();
        } catch (Throwable t) {
            friendRequestList = new ArrayList<>();
        }
        //TODO: think of moving to separate thread with specific DB executor
        try {
            friendSuggestionList = new Select().from(FriendSuggestion.class).where(FriendSuggestion_Table.pending.is(false)).queryList();
        } catch (Throwable t) {
            friendSuggestionList = new ArrayList<>();
        }
    }

    private void clearItems() {
        friendRequestList = new ArrayList<>();
        //TODO: think of moving to separate thread with specific DB executor
        try {
            new Delete().from(FriendRequest.class).where(FriendRequest_Table.pending.is(false)).query();
        } catch (Throwable t) {
        }
    }

    @Override
    public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                if (!(viewHolder instanceof FriendRequestItemViewHolder)) {
                    return;
                }
                FriendRequestsRecyclerAdapter.this.onItemRemove(viewHolder, recyclerView, swipeDir == ItemTouchHelper.RIGHT);
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                if (viewHolder != null) {
                    if (!(viewHolder instanceof FriendRequestItemViewHolder)) {
                        return;
                    }
                    getDefaultUIUtil().onSelected(((FriendRequestItemViewHolder) viewHolder).swipableLayout);
                }
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (!(viewHolder instanceof FriendRequestItemViewHolder)) {
                    return;
                }
                FriendRequestItemViewHolder friendRequestItemViewHolder = ((FriendRequestItemViewHolder) viewHolder);
                friendRequestItemViewHolder.acceptBackgroundLayout.setVisibility(View.GONE);
                friendRequestItemViewHolder.rejectBackgroundLayout.setVisibility(View.GONE);
                getDefaultUIUtil().clearView(friendRequestItemViewHolder.swipableLayout);
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (!(viewHolder instanceof FriendRequestItemViewHolder)) {
                    return;
                }
                getDefaultUIUtil().onDraw(c, recyclerView, ((FriendRequestItemViewHolder) viewHolder).swipableLayout, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (!(viewHolder instanceof FriendRequestItemViewHolder)) {
                    return;
                }
                FriendRequestItemViewHolder friendRequestItemViewHolder = ((FriendRequestItemViewHolder) viewHolder);
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && isCurrentlyActive) {
                    if (dX > 0) {
                        friendRequestItemViewHolder.acceptBackgroundLayout.setVisibility(View.VISIBLE);
                        friendRequestItemViewHolder.rejectBackgroundLayout.setVisibility(View.GONE);
                    } else if (dX < 0) {
                        friendRequestItemViewHolder.acceptBackgroundLayout.setVisibility(View.GONE);
                        friendRequestItemViewHolder.rejectBackgroundLayout.setVisibility(View.VISIBLE);
                    } else {
                        friendRequestItemViewHolder.acceptBackgroundLayout.setVisibility(View.GONE);
                        friendRequestItemViewHolder.rejectBackgroundLayout.setVisibility(View.GONE);
                    }
                } else {
                    friendRequestItemViewHolder.acceptBackgroundLayout.setVisibility(View.GONE);
                    friendRequestItemViewHolder.rejectBackgroundLayout.setVisibility(View.GONE);
                }
                getDefaultUIUtil().onDrawOver(c, recyclerView, friendRequestItemViewHolder.swipableLayout, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    void onItemRemove(final RecyclerView.ViewHolder viewHolder, final RecyclerView recyclerView, boolean accepted) {
        int position = viewHolder.getAdapterPosition();
        if (position == 0 || position == friendRequestList.size()+1) {
            return;
        }
        if (--position < friendRequestList.size()) {
            onFriendRequestRemove(friendRequestList.get(position), position, viewHolder, recyclerView, accepted);
        } else {
            position--;
            position -= friendRequestList.size();
            onFriendSuggestionRemove(friendSuggestionList.get(position), position, viewHolder, recyclerView, accepted);
        }
    }

    void onFriendSuggestionRemove(final FriendSuggestion friendSuggestion, final int listPosition, final RecyclerView.ViewHolder viewHolder, final RecyclerView recyclerView, boolean accepted) {

        final Undo undo = new Undo();

        final int adapterPosition = viewHolder.getAdapterPosition();

        Snackbar snackbar = Snackbar
                .make(recyclerView, accepted ? R.string.text_friendrequests_accepted :  R.string.text_friendrequests_rejected, Snackbar.LENGTH_LONG)
                .setActionTextColor(recyclerView.getContext().getResources().getColor(R.color.text_color_link))
                .setAction(R.string.action_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (undo.pending.compareAndSet(true, false)) {
                            friendSuggestionList.add(listPosition, friendSuggestion);
                            notifyItemInserted(adapterPosition);
                            recyclerView.scrollToPosition(adapterPosition);
                        } else {
                            Context context = recyclerView.getContext();
                            if (context instanceof ResourceListActivity) {
                                ((ResourceListActivity)context).showToast(context.getString(R.string.undo_no_longer_possible));
                            }
                        }
                    }
                });
        snackbar.getView().setBackgroundColor(accepted ? recyclerView.getContext().getResources().getColor(R.color.color_accept) : recyclerView.getContext().getResources().getColor(R.color.color_reject));

        friendSuggestionList.remove(listPosition);
        notifyItemRemoved(adapterPosition);
        snackbar.show();

        startDelayedFriendSuggestionDecision(friendSuggestion, accepted, undo, FRIENDREQUEST_UNDO_DURATION, recyclerView.getContext());
    }

    private void startDelayedFriendSuggestionDecision(final FriendSuggestion friendSuggestion, final boolean accepted, final Undo undo, final int undoDuration, final Context context) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (FetLifeApplication.getInstance().getUserSessionManager().getCurrentUser() != null) {
                    if (undo.pending.compareAndSet(true, false)) {
                        if (accepted) {
                            friendSuggestion.setPending(true);
                            friendSuggestion.save();
                            FetLifeApiIntentService.startApiCall(context, FetLifeApiIntentService.ACTION_APICALL_SEND_FRIENDREQUESTS);
                        } else {
                            friendSuggestion.delete();
                        }
                    }
                } else {
                    FetLifeApplication.getInstance().showLongToast(R.string.message_friend_decision_failed);
                }
            }
        }, undoDuration);
    }

    void onFriendRequestRemove(final FriendRequest friendRequest, final int listPosition, final RecyclerView.ViewHolder viewHolder, final RecyclerView recyclerView, boolean accepted) {

        final int adapterPosition = viewHolder.getAdapterPosition();

        final Undo undo = new Undo();

        Snackbar snackbar = Snackbar
                .make(recyclerView, accepted ? R.string.text_friendrequests_accepted :  R.string.text_friendrequests_rejected, Snackbar.LENGTH_LONG)
                .setActionTextColor(recyclerView.getContext().getResources().getColor(R.color.text_color_link))
                .setAction(R.string.action_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (undo.pending.compareAndSet(true, false)) {
                            friendRequest.setPendingState(null);
                            friendRequestList.add(listPosition, friendRequest);
                            notifyItemInserted(adapterPosition);
                            recyclerView.scrollToPosition(adapterPosition);
                        } else {
                            Context context = recyclerView.getContext();
                            if (context instanceof ResourceListActivity) {
                                ((ResourceListActivity)context).showToast(context.getString(R.string.undo_no_longer_possible));
                            }
                        }
                    }
                });
        snackbar.getView().setBackgroundColor(accepted ? recyclerView.getContext().getResources().getColor(R.color.color_accept) : recyclerView.getContext().getResources().getColor(R.color.color_reject));

        friendRequest.setPendingState(accepted ? FriendRequest.PendingState.ACCEPTED : FriendRequest.PendingState.REJECTED);
        friendRequestList.remove(listPosition);
        notifyItemRemoved(adapterPosition);
        snackbar.show();

        startDelayedFriendRequestDecision(friendRequest, friendRequest.getPendingState(), undo, FRIENDREQUEST_UNDO_DURATION, recyclerView.getContext());
    }

    private void startDelayedFriendRequestDecision(final FriendRequest friendRequest, final FriendRequest.PendingState pendingState, final Undo undo, int friendrequestUndoDuration, final Context context) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (FetLifeApplication.getInstance().getUserSessionManager().getCurrentUser() != null) {
                    if (undo.pending.compareAndSet(true, false)) {
                        friendRequest.setPending(true);
                        friendRequest.save();
                        FetLifeApiIntentService.startApiCall(context, FetLifeApiIntentService.ACTION_APICALL_SEND_FRIENDREQUESTS);
                    }
                } else {
                    FetLifeApplication.getInstance().showLongToast(R.string.message_friend_decision_failed);
                }
            }
        }, friendrequestUndoDuration);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 || position == friendRequestList.size()+1) {
            return VIEWTYPE_HEADER;
        } else {
            return VIEWTYPE_ITEM;
        }
    }

    @Override
    public FriendRequestScreenViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        if (viewType == VIEWTYPE_HEADER) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_friendrequest_header, parent, false);
            return new FriendRequestHeaderViewHolder(itemView);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_friendrequest, parent, false);
            return new FriendRequestItemViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(FriendRequestScreenViewHolder friendRequestScreenViewHolder, int position) {
        if (getItemViewType(position) == VIEWTYPE_HEADER) {
            if (!(friendRequestScreenViewHolder instanceof  FriendRequestHeaderViewHolder)) {
                Crashlytics.logException(new ClassCastException("friendRequestScreenViewHolder is not a FriendRequestHeaderViewHolder"));
                return;
            }
            onBindHeaderViewHolder((FriendRequestHeaderViewHolder) friendRequestScreenViewHolder, position == 0);
        } else if (--position < friendRequestList.size()) {
            if (!(friendRequestScreenViewHolder instanceof  FriendRequestItemViewHolder)) {
                Crashlytics.logException(new ClassCastException("friendRequestScreenViewHolder is not a FriendRequestItemViewHolder"));
                return;
            }
            onBindFriendRequestItemViewHolder((FriendRequestItemViewHolder) friendRequestScreenViewHolder, friendRequestList.get(position));
        } else {
            if (!(friendRequestScreenViewHolder instanceof  FriendRequestItemViewHolder)) {
                Crashlytics.logException(new ClassCastException("friendRequestScreenViewHolder is not a FriendRequestItemViewHolder"));
                return;
            }
            position--;
            position -= friendRequestList.size();
            onBindFriendSuggestionViewHolder((FriendRequestItemViewHolder) friendRequestScreenViewHolder, friendSuggestionList.get(position));
        }
    }

    private void onBindHeaderViewHolder(FriendRequestHeaderViewHolder friendRequestHeaderViewHolder, boolean friendRequestItem) {
        if (friendRequestItem) {
            friendRequestHeaderViewHolder.headerText.setText(R.string.header_friendrequest);
            friendRequestHeaderViewHolder.itemView.setVisibility(friendRequestList.isEmpty() ? View.GONE : View.VISIBLE);
        } else {
            friendRequestHeaderViewHolder.headerText.setText(R.string.header_friendsuggestion);
            friendRequestHeaderViewHolder.itemView.setVisibility(friendSuggestionList.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    public void onBindFriendRequestItemViewHolder(FriendRequestItemViewHolder friendRequestItemViewHolder, final FriendRequest friendRequest) {

        friendRequestItemViewHolder.headerText.setText(friendRequest.getNickname());
        friendRequestItemViewHolder.upperText.setText(friendRequest.getMetaInfo());

//        friendRequestItemViewHolder.dateText.setText(SimpleDateFormat.getDateTimeInstance().format(new Date(FriendRequest.getDate())));

        friendRequestItemViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onFriendRequestClickListener != null) {
                    onFriendRequestClickListener.onItemClick(friendRequest);
                }
            }
        });

        friendRequestItemViewHolder.avatarImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onFriendRequestClickListener != null) {
                    onFriendRequestClickListener.onAvatarClick(friendRequest);
                }
            }
        });

        friendRequestItemViewHolder.avatarImage.setImageResource(R.drawable.dummy_avatar);
        String avatarUrl = friendRequest.getAvatarLink();
        imageLoader.loadImage(friendRequestItemViewHolder.itemView.getContext(), avatarUrl, friendRequestItemViewHolder.avatarImage, R.drawable.dummy_avatar);
    }

    public void onBindFriendSuggestionViewHolder(FriendRequestItemViewHolder friendRequestItemViewHolder, final FriendSuggestion friendSuggestion) {

        friendRequestItemViewHolder.headerText.setText(friendSuggestion.getNickname());
        friendRequestItemViewHolder.upperText.setText(friendSuggestion.getMetaInfo());

//        friendRequestItemViewHolder.dateText.setText(SimpleDateFormat.getDateTimeInstance().format(new Date(FriendRequest.getDate())));

        friendRequestItemViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onFriendSuggestionClickListener != null) {
                    onFriendSuggestionClickListener.onItemClick(friendSuggestion);
                }
            }
        });

        friendRequestItemViewHolder.avatarImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onFriendSuggestionClickListener != null) {
                    onFriendSuggestionClickListener.onAvatarClick(friendSuggestion);
                }
            }
        });

        friendRequestItemViewHolder.avatarImage.setImageResource(R.drawable.dummy_avatar);
        String avatarUrl = friendSuggestion.getAvatarLink();
        imageLoader.loadImage(friendRequestItemViewHolder.itemView.getContext(), avatarUrl, friendRequestItemViewHolder.avatarImage, R.drawable.dummy_avatar);
    }

    public void refresh() {
        loadItems();
        //TODO: think of possibility of update only specific items instead of the whole list
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendRequestList.size() + friendSuggestionList.size() + 2;
    }
}

class FriendRequestScreenViewHolder extends RecyclerView.ViewHolder {
    public FriendRequestScreenViewHolder(View itemView) {
        super(itemView);
    }
}

class FriendRequestHeaderViewHolder extends FriendRequestScreenViewHolder {

    TextView headerText;

    public FriendRequestHeaderViewHolder(View itemView) {
        super(itemView);
        headerText = (TextView) itemView.findViewById(R.id.friendrequest_header);
    }
}


class FriendRequestItemViewHolder extends FriendRequestScreenViewHolder {

    ImageView avatarImage;
    TextView headerText, upperText, dateText;
    View swipableLayout, acceptBackgroundLayout, rejectBackgroundLayout;

    public FriendRequestItemViewHolder(View itemView) {
        super(itemView);

        headerText = (TextView) itemView.findViewById(R.id.friendrequest_header);

        swipableLayout = itemView.findViewById(R.id.swipeable_layout);
        acceptBackgroundLayout = itemView.findViewById(R.id.friendrequest_accept_layout);
        rejectBackgroundLayout = itemView.findViewById(R.id.friendrequest_reject_layout);

        upperText = (TextView) itemView.findViewById(R.id.friendrequest_upper);
        dateText = (TextView) itemView.findViewById(R.id.friendrequest_right);
        avatarImage = (ImageView) itemView.findViewById(R.id.friendrequest_icon);
    }
}