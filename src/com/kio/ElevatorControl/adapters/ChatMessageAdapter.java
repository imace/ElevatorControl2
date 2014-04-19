package com.kio.ElevatorControl.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.kio.ElevatorControl.R;
import com.kio.ElevatorControl.models.ChatMessage;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.TextView;

import java.util.List;

/**
 * Created by keith on 14-4-7.
 * User keith
 * Date 14-4-7
 * Time 下午5:27
 */
public class ChatMessageAdapter extends BaseAdapter {

    private Activity baseActivity;

    private List<ChatMessage> chatMessageList;

    public ChatMessageAdapter(Activity activity, List<ChatMessage> chatList) {
        this.baseActivity = activity;
        this.chatMessageList = chatList;
    }

    @Override
    public int getCount() {
        return chatMessageList.size();
    }

    @Override
    public ChatMessage getItem(int position) {
        return chatMessageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater mInflater = LayoutInflater.from(baseActivity);
        ChatMessage item = getItem(position);
        if (item.isSend()) {
            SendViewHolder sendViewHolder;
            View view;
            if (convertView == null) {
                ViewGroup viewGroup = (ViewGroup) mInflater.inflate(R.layout.chat_send_message_item, null);
                sendViewHolder = new SendViewHolder();
                sendViewHolder.messageContent = (TextView) viewGroup.findViewById(R.id.message_content);
                viewGroup.setTag(sendViewHolder);
                view = viewGroup;
            } else {
                sendViewHolder = (SendViewHolder) convertView.getTag();
                view = convertView;
            }
            sendViewHolder.messageContent.setText(item.getMessage());
            return view;
        } else {
            ReceiveViewHolder receiveViewHolder;
            View view;
            if (convertView == null) {
                ViewGroup viewGroup = (ViewGroup) mInflater.inflate(R.layout.chat_receive_message_item, null);
                receiveViewHolder = new ReceiveViewHolder();
                receiveViewHolder.messageContent = (TextView) viewGroup.findViewById(R.id.message_content);
                viewGroup.setTag(receiveViewHolder);
                view = viewGroup;
            } else {
                receiveViewHolder = (ReceiveViewHolder) convertView.getTag();
                view = convertView;
            }
            receiveViewHolder.messageContent.setText(item.getMessage());
            return view;
        }
    }

    /**
     * Message Send View Holder
     */
    private class SendViewHolder {
        TextView messageContent;
    }

    /**
     * Message Receive View Holder
     */
    private class ReceiveViewHolder {
        TextView messageContent;
    }
}
