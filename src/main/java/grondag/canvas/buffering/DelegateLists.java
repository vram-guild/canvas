/*******************************************************************************
 * Copyright 2019 grondag
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.buffering;

import java.util.concurrent.ArrayBlockingQueue;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DelegateLists {
    private static final ArrayBlockingQueue<ObjectArrayList<DrawableDelegate>> delegateLists = new ArrayBlockingQueue<>(
            4096);

    static ObjectArrayList<DrawableDelegate> getReadyDelegateList() {
        ObjectArrayList<DrawableDelegate> result = delegateLists.poll();
        if (result == null)
            result = new ObjectArrayList<>();
        return result;
    }

    static void releaseDelegateList(ObjectArrayList<DrawableDelegate> list) {
        if (!list.isEmpty())
            list.clear();
        delegateLists.offer(list);
    }
}
