package com.sk.rts.application.tools;

import java.util.ArrayList;
import java.util.List;

public class UpdateRemarkBuilder {
    private final List<UpdateItem> items = new ArrayList<>();

    public void add(String fieldName, Object oldValue, Object newValue) {
        items.add(new UpdateItem(fieldName, oldValue, newValue));
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            UpdateItem item = items.get(i);

            sb.append(item.fieldName);
            sb.append(":");
            sb.append(item.oldValue);
            sb.append("->");
            sb.append(item.newValue);
        }
        return sb.toString();
    }

    private static class UpdateItem {
        private final String fieldName;
        private final Object oldValue;
        private final Object newValue;

        public UpdateItem(String fieldName, Object oldValue, Object newValue) {
            this.fieldName = fieldName;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}
