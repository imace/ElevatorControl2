package com.inovance.elevatorcontrol.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.inovance.elevatorcontrol.R;
import com.inovance.elevatorcontrol.models.ChatMessage;
import com.inovance.elevatorcontrol.views.SquareLayout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by keith on 14-4-7.
 * User keith
 * Date 14-4-7
 * Time 下午5:27
 */
public class ChatMessageAdapter extends BaseAdapter {

    private static final String TAG = ChatMessageAdapter.class.getSimpleName();

    private static final int SEND_TYPE = 0;

    private static final int RECEIVE_TYPE = 1;

    static int TEXT_TYPE_COLOR = 0xFFA9CC27;

    static int PROFILE_TYPE_COLOR = 0xFF6F4761;

    static int PICTURE_TYPE_COLOR = 0xFF8FF6D9;

    static int VIDEO_TYPE_COLOR = 0xFFEDE244;

    static int AUDIO_TYPE_COLOR = 0xFFE6083B;

    private OnMessageItemClickListener mListener;

    public interface OnMessageItemClickListener {
        void onClick(View view, int position, ChatMessage message);
    }

    public void setOnMessageItemClickListener(OnMessageItemClickListener listener) {
        mListener = listener;
    }

    private List<ChatMessage> chatMessageList = new ArrayList<ChatMessage>();

    private LayoutInflater mInflater;

    public ChatMessageAdapter(Context context, List<ChatMessage> messageList) {
        this.chatMessageList = messageList;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void updateChatMessageList(List<ChatMessage> chatList) {
        // Collections.sort(chatList, new SortComparator());
        this.chatMessageList.clear();
        this.chatMessageList.addAll(chatList);
        notifyDataSetChanged();
    }

    public void addChatMessageList(List<ChatMessage> chatList) {
        this.chatMessageList.addAll(chatList);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return chatMessageList.size();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).getChatType() == ChatMessage.SEND) {
            return SEND_TYPE;
        }
        if (getItem(position).getChatType() == ChatMessage.RECEIVE) {
            return RECEIVE_TYPE;
        }
        return SEND_TYPE;
    }

    @Override
    public ChatMessage getItem(int position) {
        return chatMessageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        int rowType = getItemViewType(position);
        if (convertView == null) {
            holder = new ViewHolder();
            switch (rowType) {
                case SEND_TYPE:
                    convertView = mInflater.inflate(R.layout.chat_send_message_item, null);
                    holder.contentView = convertView.findViewById(R.id.send_content_view);
                    holder.iconBackgroundView = (SquareLayout) convertView.findViewById(R.id.send_icon_view);
                    holder.iconView = (ImageView) convertView.findViewById(R.id.send_type_icon);
                    holder.titleView = (TextView) convertView.findViewById(R.id.send_chat_title);
                    holder.phoneNumberView = (TextView) convertView.findViewById(R.id.send_phone_number);
                    holder.timeView = (TextView) convertView.findViewById(R.id.send_time);
                    break;
                case RECEIVE_TYPE:
                    convertView = mInflater.inflate(R.layout.chat_receive_message_item, null);
                    holder.contentView = convertView.findViewById(R.id.receive_content_view);
                    holder.iconBackgroundView = (SquareLayout) convertView.findViewById(R.id.receive_icon_view);
                    holder.iconView = (ImageView) convertView.findViewById(R.id.receive_type_icon);
                    holder.titleView = (TextView) convertView.findViewById(R.id.receive_chat_title);
                    holder.phoneNumberView = (TextView) convertView.findViewById(R.id.receive_phone_number);
                    holder.timeView = (TextView) convertView.findViewById(R.id.receive_time);
                    break;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final ChatMessage item = getItem(position);
        final int index = position;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeString;
        try {
            Date date = new Date(Long.parseLong(item.getTimeString()) * 1000);
            timeString = dateFormat.format(date);
        } catch (Exception e) {
            timeString = dateFormat.format(new Date());
        }
        holder.contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onClick(view, index, item);
                }
            }
        });
        holder.titleView.setText(item.getTitle());
        switch (item.getChatType()) {
            case ChatMessage.SEND:
                holder.phoneNumberView.setText("To:" + item.getToNumber());
                break;
            case ChatMessage.RECEIVE:
                holder.phoneNumberView.setText("From:" + item.getFromNumber());
                break;
        }
        holder.timeView.setText(timeString);
        switch (item.getContentType()) {
            case ChatMessage.TYPE_TEXT:
                holder.iconBackgroundView.setBackgroundColor(TEXT_TYPE_COLOR);
                holder.iconView.setImageResource(R.drawable.chat_type_text);
                break;
            case ChatMessage.TYPE_PROFILE:
                holder.iconBackgroundView.setBackgroundColor(PROFILE_TYPE_COLOR);
                holder.iconView.setImageResource(R.drawable.chat_type_profile);
                break;
            case ChatMessage.TYPE_PICTURE:
                holder.iconBackgroundView.setBackgroundColor(PICTURE_TYPE_COLOR);
                holder.iconView.setImageResource(R.drawable.chat_type_picture);
                break;
            case ChatMessage.TYPE_VIDEO:
                holder.iconBackgroundView.setBackgroundColor(VIDEO_TYPE_COLOR);
                holder.iconView.setImageResource(R.drawable.chat_type_video);
                break;
            case ChatMessage.TYPE_AUDIO:
                holder.iconBackgroundView.setBackgroundColor(AUDIO_TYPE_COLOR);
                holder.iconView.setImageResource(R.drawable.chat_type_audio);
                break;
        }
        return convertView;
    }

    private static class ViewHolder {
        View contentView;
        SquareLayout iconBackgroundView;
        ImageView iconView;
        TextView titleView;
        TextView phoneNumberView;
        TextView timeView;
    }

    private class SortComparator implements Comparator<ChatMessage> {

        @Override
        public int compare(ChatMessage object1, ChatMessage object2) {
            long time1 = Long.parseLong(object1.getTimeString());
            long time2 = Long.parseLong(object2.getTimeString());
            if (time1 > time2) {
                return -1;
            } else if (time1 < time2) {
                return 1;
            } else {
                return 0;
            }
        }

    }
}
