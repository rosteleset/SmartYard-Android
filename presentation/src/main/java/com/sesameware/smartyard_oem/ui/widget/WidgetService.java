package com.sesameware.smartyard_oem.ui.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * @author Nail Shakurov
 * Created on 13.05.2020.
 */
public class WidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetFactory(getApplicationContext(), intent);
    }

}