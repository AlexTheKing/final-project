package com.example.alex.restaurantx.backend.database.models;

import com.example.alex.restaurantx.backend.database.annotations.Table;
import com.example.alex.restaurantx.backend.database.annotations.dbPrimaryKey;
import com.example.alex.restaurantx.backend.database.annotations.dbReal;
import com.example.alex.restaurantx.backend.database.annotations.dbText;

@Table(value = "dishes")
public class DishModel {

    @dbText
    public static final String TYPE = "type";

    @dbText
    @dbPrimaryKey
    public static final String NAME = "name";

    @dbText
    public static final String WEIGHT = "weight";

    @dbReal
    public static final String COST = "cost";

    @dbText
    public static final String CURRENCY = "currency";

    @dbText
    public static final String DESCRIPTION = "description";

    @dbText
    public static final String INGREDIENTS = "ingredients";

    @dbReal
    public static final String AVERAGE_ESTIMATION = "average_estimation";

    @dbText
    public static final String BITMAP_URL = "bitmap_url";
}
