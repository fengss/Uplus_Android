# Uplus_Android
uplus的android版!用java的ssh框架写出一套即时通讯web应用 基本也有朋友圈的小功能,而android端就是这个,2014年作品

#此app 是基于现即时通讯软件流行的xmpp协议

## xmpp+asmack+openfire

服务器在web端开放了9090以供web后台使用

![image](https://github.com/fengss/Uplus_Android/blob/master/img/1.jpg?raw=true)
<br/>
![image](https://github.com/fengss/Uplus_Android/blob/master/img/2.jpg?raw=true)

#接下来介绍整个app构架

<br/>
![image](https://github.com/fengss/Uplus_Android/blob/master/img/3.jpg?raw=true)
我先排除掉不介绍的二次开发的ui

- xlistview：

里面有listhead和listfoot还有聊天窗口的listview和朋友圈的listview，主要用来下拉刷新下来加载更多

- pulltorefresh：

这个是用于联系人列表的刷新

- iphoneiphonetreeview:

继承了ExpandableListView主要用于负责联系人组和联系人的数据

- quickaction:

集合popupwindow的弹窗

- waterfall:

用于朋友圈的弹窗

- image_friend和model_friend:

用户朋友圈的数据获取和图片缓存本地

 

 

接下来主要介绍重点包

- activity

app上各个主要的activity，主要如:login,register,main(联系人界面,会话窗口),chat,friend(朋友圈界面),set(设置界面),rote(百度地图)

附带一个baseactivity

- adapter

这个包里面主要有，联系人的adapter,聊天数据的adapter,会话窗口adapter还有一个朋友圈adapter

- baidubls

主要放一些百度地图api御用的activity

- db

两个provider,一个是聊天的数据一个是联系人数据，朋友圈有待加入sqlite本地数据库，联系人一旦更新会notifyChange

联系人的数据库在mainactivity注册ContentObserver处理代码

- fragment

主要用于mainactivity下的一些分界面

比如联系人fragment，会话窗口fragment

- receiver

主要放的广播接收是网络改变和关机

一旦网络改变则激发网络状态改变，从而通知ui或服务更改状态

关机则停止后台服务

- smack

这个主要asmack的jar后续写自己合适的方法

主要一些登陆，注册，新消息处理，发送消息处理， 处理心跳包，文件的收发，联系人的更新

还有一些注册的监听：注册新消息监听， 注册消息发送失败监听，注册服务器回应ping消息监听， 发送离线消息，注册文件接收初始化

注册的监听会每次在静态代码块配置好config然后用户激发login将以上监听全部注

同道理，logout登出的时候移除此用户所有监听

新消息和发送消息的处理还有联系人的处理都会写入sqlite

- service:

主要写一些基于smack定义的方法全都写了一遍，然后service定义与activity线程代码执行完后的反馈


ui包：

接下来的ui是一些小改动的ui不介绍

util：

处理一些时间戳和preferenceconstants的工具和字段

 
![image](https://github.com/fengss/Uplus_Android/blob/master/img/4.jpg?raw=true)<br/>
![image](https://github.com/fengss/Uplus_Android/blob/master/img/5.jpg?raw=true)<br/>
![image](https://github.com/fengss/Uplus_Android/blob/master/img/6.jpg?raw=true)<br/>
![image](https://github.com/fengss/Uplus_Android/blob/master/img/7.jpg?raw=true)<br/>
![image](https://github.com/fengss/Uplus_Android/blob/master/img/8.jpg?raw=true)<br/>
![image](https://github.com/fengss/Uplus_Android/blob/master/img/9.jpg?raw=true)<br/>
![image](https://github.com/fengss/Uplus_Android/blob/master/img/10.jpg?raw=true)<br/>


## 所遇到的困惑

我在处理语音通话的时候也想到了sip的sipboard

但是还是在xmpp上尝试

我用speek对话筒录制的声音编码十六进制文件

然后想通过asmack所提供的

但是状态一直阻塞在 negotiating stream

看了好多资料说改写`org.jivesoftware.smackx.filetransfer.Socks5TransferNegotiator.discoverLocalIP()`方法

我的是`asmack-android-6-0.8.1.1.jar`根本没有这个玩意

又看到说改写`createIncomingStream`,可是改写还是没有用啊！

我无奈只好写了http协议上传

我的想法是这样，按下激发录音,`ontouch`抬起松开创建发送文件的线程,然后`sendPacket`

`http`链接，用户在注册监听新消息的代码处理语音特殊的链接,下载然后cache到本地,并且路径写入sqlite，chat界面的`simplecursoradapter`的`getview`

如果是链接则会speek播放本地的speek编码文件

参照好多大牛说改写`org.jivesoftware.smackx.filetransfer.Socks5TransferNegotiator.discoverLocalIP()`方法。

我搞的源码的`Socks5TransferNegotiato`r这个类里根本没有`discoverLocalIP()`方法!



接下来是web端，采用了php的thinkphp框架  

![image](https://github.com/fengss/Uplus_Android/blob/master/img/11.jpg?raw=true)<br/>



