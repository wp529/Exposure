[![](https://jitpack.io/v/wp529/Exposure.svg)](https://jitpack.io/#wp529/Exposure)
### Exposure

Exposure是一个很方便对View进行曝光埋点收集的库，不用修改现有布局实现方式，只需在现有代码上做极少量修改即可实现View的曝光埋点。支持判断View是否达到有效曝光面积，支持RecyclerView的线性布局、网格布局、瀑布流布局，横向滑动曝光埋点，支持对指定View进行曝光收集。

**添加依赖:**

```groovy
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
dependencies {
	implementation 'com.github.wp529:Exposure:1.2.1'
}
```

###### 使用方式：

1. 将需要采集曝光的View替换为对应的曝光View，库里面提供了三个曝光View ***(ExposureLinearLayout，ExposureFrameLayout，ExposureRelativeLayout)***，若需其他类型的曝光View可以自行让对应View实现IProvideExposureData接口

   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <com.wp.exposure.view.ExposureFrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
       xmlns:tools="http://schemas.android.com/tools"
       android:id="@+id/exposureRoot"
       android:layout_width="match_parent"
       android:layout_height="wrap_content"
       android:orientation="vertical">
       <TextView
           android:id="@+id/tvText"
           android:layout_width="wrap_content"
           android:layout_height="wrap_content"
           tools:text="TEXT" />
   </com.wp.exposure.view.ExposureLinearLayout>
   ```

2. 为曝光View绑定上对应的曝光数据

   ```kotlin
   //注:这里的data对象最好实现了equals方法
   exposureRoot.exposureBindData = data
   ```
### RecyclerView添加曝光收集
在给RecyclerView设置完adapter后实例化RecyclerViewExposureHelper，实例化时需传递五个参数
* recyclerView 需要收集曝光的RecyclerView
* exposureValidAreaPercent 判定曝光的面积,即大于这个面积才算做曝光,百分制,eg:设置为50 item的面积为200平方,则必须要展示200 * 50% = 100平方及以上才算为曝光
* lifecycleOwner RecyclerView感知此生命周期组件,根据生命周期感知RV可见性,以便自动处理开始曝光和结束曝光,一般情况RV在Activity中传Activity,在Fragment中传Fragment
* mayBeCoveredViewList 可能会遮挡RV的View集合
* exposureStateChangeListener 曝光状态改变监听器
   ```kotlin
   recyclerViewExposureHelper =
            RecyclerViewExposureHelper(
                recyclerView = rvList,
                exposureValidAreaPercent = 50,
                lifecycleOwner = this,
                mayBeCoveredViewList = null,
                exposureStateChangeListener = object : IExposureStateChangeListener<String> {
                    override fun onExposureStateChange(
                        bindExposureData: String,
                        position: Int,
                        inExposure: Boolean
                    ) {
                        Log.i(
                            "ListActivity", "${bindExposureData}${
                                if (inExposure) {
                                    "开始曝光"
                                } else {
                                    "结束曝光"
                                }
                            }"
                        )
                    }
                }
            )
   ```
其他情况：若RecyclerView被嵌套在可滚动控件(eg:ScrollView,NestedScrollView,RecyclerView等)中，将会导致RecyclerViewExposureHelper中持有的RecyclerView不能响应滑动的情况,就必须由外部告知RecyclerView滚动状态然后触发曝光收集。具体做法：给可滚动控件添加滚动监听，滚动监听中调用recyclerViewExposureHelper.onScroll()

### 对指定View添加曝光收集
实例化ViewExposureHelper，实例化时需传递五个参数，与RecyclerViewExposureHelper基本一致
* viewList 需要指定收集曝光的View集合

   ```kotlin
      val helper = ViewExposureHelper(
            viewList = list,
            exposureValidAreaPercent = 50,
            lifecycleOwner = this,
            mayBeCoveredViewList = null,
            exposureStateChangeListener = object : IExposureStateChangeListener<String> {
                @SuppressLint("LongLogTag")
                override fun onExposureStateChange(
                    bindExposureData: String,
                    position: Int,
                    inExposure: Boolean
                ) {
                    Log.i(
                        "CollectViewGroupActivity", "${bindExposureData}${
                            if (inExposure) {
                                "开始曝光"
                            } else {
                                "结束曝光"
                            }
                        }"
                    )
                }
            }
        )
   ```
   
1.若指定收集曝光的View在可滚动的布局中,那么在滚动时,需调用helper.onScroll()

2.若指定收集曝光的View Visible状态变更,helper.childViewVisibleChange()

3.若需动态添加指定收集曝光的View,则需调用helper.addViewToRecordExposure(view)

***

至此，曝光收集接入完成，只需在**onExposureStateChange**回调中进行处理即可。**onExposureStateChange**回调会回传三个参数。
1. view绑定的曝光数据
2. 曝光状态改变的位置
3. 曝光状态。曝光状态true代表view从未曝光状态进入曝光中状态，false代表view从曝光中状态进入结束曝光状态

View曝光埋点对客户端来说只用处理三个问题,此库的作用即是处理这三个问题
* view可见(开始曝光)

* view不可见(结束曝光)

* view可见面积是否为有效曝光面积

埋点SDK一般会提供三个api供客户端调用

* onItemExposureStart()

* onItemExposureEnd()

* onItemExposureUpload()

所以只需在**onExposureStateChange**里调用埋点SDK对应的api即可,至于曝光时长是否为有效曝光一般由埋点SDK进行计算

若没有埋点SDK，也可通过**onExposureStateChange**自行处理相关逻辑

##### 可查看demo获知更多使用姿势

