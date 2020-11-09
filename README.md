### ExposureRecyclerView

​	ExposureRecyclerView是一个封装了item曝光逻辑的RecyclerView，可以很方便的对RecyclerView进行曝光埋点。支持线性布局、网格布局、瀑布流布局。支持配置有效曝光面积和有效曝光时长。

**添加依赖:**
```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
dependencies {
	implementation 'com.github.wp529:ExposureRecyclerView:1.0.1'
}
```

**添加ExposureRecyclerView布局**

```
<com.wp.exposure.ExposureRecyclerView
        android:id="@+id/rvList"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:exposure_threshold="500"
        app:exposure_valid_area="50%"
        app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        tools:listitem="@layout/item_list" />
```

其中有两个属性用于判定是否为有效曝光

1. exposure_threshold，曝光有效时长。即item一次展示大于这个值才记为一次有效曝光,单位毫秒
2. exposure_valid_area，曝光有效面积。即可见区域大于这个面积才算做曝光。eg:item的总面积为100平方，设置为50%则代表item可见面积在50平方以上才算为曝光。0%则代表只要出现就算为曝光，100%则代表完全展示才算为曝光

在Activity/Fragment中需调用

```
//须告知ExposureRecyclerView可见和不可见的状态切换以使得ExposureRecyclerView可以正确进行曝光相关的数据收集
一般在Activity/Fragment的对应生命周期方法中调用即可
ExposureRecyclerView.onResume()
ExposureRecyclerView.onPause()
```

**获取曝光结果有两种方式，由数据统计SDK所需的方式其一选择即可**

1. 数据统计SDK内部处理了曝光统计逻辑。即只用告知数据统计SDK什么时候item开始曝光、什么时候结束曝光的此类SDK，可使用如下方式

   ```kotlin
   //给ExposureRecyclerView设置IExposureStateChangeListener
   interface IExposureStateChangeListener {
       /**
        * 曝光状态变更
        * @param position 曝光状态变更的位置
        * @param inExposure true为从非曝光状态转为曝光状态,false为从曝光状态转为非曝光状态
        */
       fun onExposureStateChange(position: Int, inExposure: Boolean)
   }
   ```

2. 不参与曝光数据的统计，只负责上报曝光结果的一类SDK

   ```kotlin
   /**
    * 此方法获取RecyclerView的曝光结果
    * @param needResetAlreadyExposureList 是否需要重置已曝光列表
    * @return 曝光结果数组
    */
   ExposureRecyclerView.getAlreadyExposureData(needResetAlreadyExposureList: Boolean = true): List<AlreadyExposureData>

   //曝光结果为此模型的数组
   data class AlreadyExposureData(
     	// 曝光item的位置
       var position: Int,
     	// item曝光时长数组，列表在滑动过程中一个item可能不止进行一次曝光
       // 所以此数组中的每个值为每次曝光的时长
       var exposureTimeList: ArrayList<Long>
   )
   ```



