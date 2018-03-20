package com.example.android.bakingbuddy.data;

import android.content.Context;
import android.graphics.Movie;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.android.bakingbuddy.R;
import com.example.android.bakingbuddy.model.Recipe;

import java.util.ArrayList;


/**
 * Created by pkennedy on 3/18/18.
 */

public class MasterListAdapter extends RecyclerView.Adapter<MasterListAdapter.ViewHolder> {

    private Context mContext;

    private MasterListAdapterClickListener mListener;

    // I'll put some placeholder data here to use for testing
    //TODO: Replace these with resources from api
    private int[] images = { R.drawable.nutella_pie, R.drawable.brownies, R.drawable.yellow_cake, R.drawable.cheesecake };

    // ArrayList of Recipes
    private ArrayList<Recipe> mRecipes = new ArrayList<>();

    /**
     * The interface for custom RecyclerViewClickListener
     *
     */
    public interface MasterListAdapterClickListener {
        void onClick(View view, Recipe recipe);
    }

    public MasterListAdapter(MasterListAdapterClickListener listener, Context context){
        this.mListener = listener;
        this.mContext = context;
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public ImageView itemImage;
        public TextView itemName;
        public TextView itemServes;

        public ViewHolder(View itemView, MasterListAdapterClickListener listener){
            super(itemView);
            itemImage = (ImageView) itemView.findViewById(R.id.iv_master_list_item);
            itemName = (TextView) itemView.findViewById(R.id.tv_master_list_item_recipe_name);
            itemServes = (TextView) itemView.findViewById(R.id.tv_master_list_item_recipe_serves);
            mListener = listener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mListener.onClick(view, mRecipes.get(getAdapterPosition()));
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_layout_master_list_item, parent, false);
        return new ViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.itemImage.setImageResource(images[position]);
        holder.itemName.setText(mRecipes.get(position).getName());
        holder.itemServes.setText(mRecipes.get(position).getServings());
    }

    @Override
    public int getItemCount() {
        if (null == mRecipes) return 0;
        return mRecipes.size();
    }

    public void setRecipes(ArrayList<Recipe> recipes){
        mRecipes = recipes;
        notifyDataSetChanged();
    }
}
