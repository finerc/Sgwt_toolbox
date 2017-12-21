package com.example.pc.eliminating_pqb;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.pc.eliminating_pqb.User;

import java.util.List;

/**
 * Created by pc on 2017/11/7.
 */

public class UserAdapter extends ArrayAdapter<User> {

    private int resourceId;

    public UserAdapter(Context context, int _resourceId, List<User> objects) {
        super(context, _resourceId, objects);
        resourceId = _resourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        User user = getItem(position);
        View view;
        ViewHolder viewHolder;
        if(convertView==null){
            view = LayoutInflater.from(getContext()).inflate(resourceId,parent,false);
            viewHolder = new ViewHolder();
            viewHolder.userName = (TextView)view.findViewById(R.id.name_for_display);
            viewHolder.userScore = (TextView)view.findViewById(R.id.score_for_display);
            viewHolder.userRank= (TextView)view.findViewById(R.id.ranks);
            view.setTag(viewHolder);
        }else{
            view = convertView;
            viewHolder = (ViewHolder)view.getTag();
        }
        viewHolder.userName.setText(user.getName());
        viewHolder.userScore.setText(Integer.toString(user.getScore()));
        viewHolder.userRank.setText(Integer.toString(user.getRank()));
        return view;
    }

    class ViewHolder{
        TextView userName;
        TextView userScore;
        TextView userRank;
    }
}
