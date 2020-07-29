#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <EEPROM.h>
#include <ArduinoJson.h>
#include <TimeLib.h>
#include <WiFiUdp.h> 


WiFiClient espClient;
PubSubClient client(espClient);

/**
 *   NTP
 */
static const char ntpServerName[] = "time1.aliyun.com";//阿里云的时间服务器
const int timeZone = 8;     // 时区

/**
 *   wifi  UDP
 */
WiFiUDP Udp;
unsigned int localPort = 8888;  // local port to listen for UDP packets
time_t getNtpTime();

int time_second,time_minute ;
void timeJudge();

#define LED      2          //网络状态指示LED
#define setPin   5         //继电器
#define readPin  4         //按键检测
#define led_show 13        //LED 指示
int  buttonTimeCheck =0;  //按键时间检测


char  *wifissid  = "Sl" ;          //wifi 名称
char  *password  ="sl1243957257" ;//wifi 密码

#define debug_log               //打开调试输出日志

int flag; //LED 状态标志

/**********************************************************
 * 
 * 
 * onenet 服务器相关配置
 * 
 * /
/*服务器设置*/
#define mqtt_server "183.230.40.39" //服务器IP
#define mqtt_port    6002           //端口号
/*登陆设置及设备绑定*/
#define Product_id  "354343"        //产品ID，登陆用户
#define API_key     "5RJbtFc8PaR2gz=7ALw30a80NlM=" //登录密码
#define Device_id   "607071188"            //登录设备ID

/*主题订阅*/
char *sub_topic = "esp8266" ;  //订阅主题    默认配置时需要
char *pub_topic = "app" ;     //发布主题

const char* device_topic;
const char* app_topic;
long  set_month ;
long  set_day ;
long  set_hour ;
long  set_minute ;
long set_times ;

/************************************************************
 * 
 * EEPROM  配置信息保存
 * 
 */
struct config_type
{
  char stassid[32];  //WIFI 名称
  char stapsw[64];   //wifi 密码
  char mqtt_pub_id[32];//发布主题
  char mqtt_sub_id[32];//订阅主题
};
config_type config;

/*******保存信息函数******************/
void saveConfig()
{
  EEPROM.begin(512);
  uint8_t *p = (uint8_t*)(&config);
  for (int i = 0; i < sizeof(config); i++)
  {
    EEPROM.write(i, *(p + i));
  }
  EEPROM.commit();
}

/*******加载保存函数***********************/
void loadConfig()
{
  EEPROM.begin(512);
  uint8_t *p = (uint8_t*)(&config);
  for (int i = 0; i < sizeof(config); i++)
  {
    *(p + i) = EEPROM.read(i);
  }
  EEPROM.commit();
}

/*****************************************************************
 * 
 * 智能配网函数
 * 
 */

void smartConfig()
{
  WiFi.mode(WIFI_STA);
#ifdef debug_log
  Serial.println("\r\nWait for Smartconfig");
#endif 
  //调用smartconfig功能
  WiFi.beginSmartConfig();
  while (1)
  {
    Serial.print(".");
    digitalWrite(LED, 0);
    delay(300);
    digitalWrite(LED, 1);
    delay(300);
    //若配置完成打印获取到的ssid
    if (WiFi.smartConfigDone())
    { 
      strcpy(config.stassid, WiFi.SSID().c_str());
      strcpy(config.stapsw, WiFi.psk().c_str());
      saveConfig();
      #ifdef debug_log
         Serial.println("SmartConfig Success");
         Serial.printf("SSID:%s\r\n", WiFi.SSID().c_str());
         Serial.printf("PSW:%s\r\n", WiFi.psk().c_str());
      #endif
      break;
    }
  }
}

/**************************************************************
 * 
 * 相关配置初始化
 * 
 */
void setup() 
{
  pinMode(2, OUTPUT);     
  pinMode(setPin, OUTPUT);
  pinMode(readPin, INPUT);
  pinMode(led_show,OUTPUT);

  digitalWrite(setPin,0);//继电器闭合

  Serial.begin(115200);
  while(Serial.read()>= 0){}//clear serialport
  
  setup_wifi();     //连网  
  client.setServer(mqtt_server,mqtt_port); //连接服务器
  client.setCallback(callback);            //注册回调
}



/**************************************************************
 * 
 * wifi 连接初始化
 * 
 * 
 */
void setup_wifi() 
{
  Serial.println();
  loadConfig() ;
  #ifdef debug_log
    Serial.println("Connecting to ....");
    Serial.println("read config");
    Serial.print("pub_topic is:");
    Serial.println(config.mqtt_pub_id);
    Serial.print("sub_topic is:");
    Serial.println(config.mqtt_sub_id);
    Serial.print("wifi ssid is:");
    Serial.println(config.stassid);
    Serial.print("wifi password is:");
    Serial.println(config.stapsw);
  #endif

  wifissid = config.stassid; //将eeprom里的数据读出来 在连接网络
  password = config.stapsw;

  /*sub_topic = config.mqtt_sub_id;
  pub_topic = config.mqtt_pub_id;
*/
  WiFi.begin(wifissid,password);  //等待wifi连接

  int times =0;
  while(WiFi.status() != WL_CONNECTED)
  {
    Serial.print(".");
    flag =~flag ;
    digitalWrite(LED, flag);
    times++;
    delay(1000);
    Serial.print(times);
    if(times==12)
    {
        break;   //12s 内还未连接到网络
    } 
  }
  Serial.print("\r\n");
  while(times==12)  //模块自动进入samartconfig 模式
  {
      smartConfig(); 
      if (WiFi.smartConfigDone())
      {
        times=0;
        break ;
      }     
    
  }
  
  while(WiFi.status() == WL_CONNECT_FAILED)
  {
  /*  if(WiFi.status() != WL_NO_SSID_AVAIL) //如果无法访问已配置的SSID
    { 
       Serial.print("in");
       smartConfig();
       if (WiFi.smartConfigDone())
       {
        break ;
        }   
    }*/
      smartConfig(); 
      if (WiFi.smartConfigDone())
      {
        break ;
      }     
  }

  Serial.println("WiFi connected");//连接成功
  Serial.print("IP address: ");
  Serial.println(WiFi.localIP());
  digitalWrite(LED,1);  //配置成功后熄灭
 // saveConfig();

/**
 *    NTP 相关配置
 */
  Udp.begin(localPort);  

  #ifdef debug_log
    Serial.println(Udp.localPort());
    Serial.println("waiting for sync");
  #endif
    setSyncProvider(getNtpTime);
    setSyncInterval(300);

}

/***********************************************************************
 * mqtt 回调函数
 * 
 */

void callback(char* topic, byte* payload, unsigned int length) 
{
  String msg="";
  String cmd="";
  String pub_id="";
  for (int i = 0; i < length; i++) 
  {
    msg+= (char)payload[i];
  }

 digitalWrite(LED,0); 
/**
 *   json 解析数据
 */

  DynamicJsonDocument  jsonBuffer(200);
  deserializeJson(jsonBuffer, msg);
  JsonObject root = jsonBuffer.as<JsonObject>();


  device_topic = root["topic"];
  app_topic = root["user"];
  set_month = root["month"];
  set_day = root["day"];
  set_hour = root["hour"];
  set_minute = root["minute"];
  set_times = root["times"];
  
/*
  Serial.println();
  Serial.println(device_topic);
  Serial.println(app_topic);

  Serial.println(set_month);
  Serial.println(set_day);
  Serial.println(set_hour);
  Serial.println(set_minute);
  Serial.println(set_times);
  */
  
}

  /*******************************************************************
   * 
   * mqtt重新连接
   */
void reconnect() 
{
  while (!client.connected()) 
  {
    if (client.connect(Device_id,Product_id,API_key)) //连接服务器
    {
      //Serial.println("service connect success");//连接成功
      //连接成功以后就开始订阅
      if(client.subscribe(sub_topic,1))
      {
        strcpy(config.mqtt_sub_id,sub_topic); 
        saveConfig();  //保存订阅信息
        #ifdef debug_log
           Serial.print("sub topic success: ");//订阅成功  
           Serial.println(config.mqtt_sub_id);//订阅主题信息
        #endif
      }
      else
      {
       #ifdef debug_log
        Serial.println("sub topic defeated");//订阅失败
      #endif
      }
    } 
    else 
    {
      Serial.println("service connect error");//连接失败
      delay(3000);  //3s 后重连
    }
  }
}

/**
 * 通过查询数据尾标志数据判断接收完一个完整数据包
 * 
 */
uint8_t  start_flag=0,check=0;
 
void loop() 
{

    /** MQTT 相关*/
  if (!client.connected()) 
  {
    reconnect();
  }
  client.loop();

    /** NTP 相关*/
  now();
  timeJudge();

 if (start_flag==1)
 {
   time_second++;
  if (time_second>59)
  {
    time_second=0;
    time_minute++;
  }

 }

    /** 按键检测相关 */
 if (digitalRead(readPin)==LOW)  
 {
 /*    
  Serial.print("时间1 -->");
  Serial.print(time_second);
  Serial.print(":");
  Serial.print(time_minute);
  Serial.print("\n\r");
*/
  if (check==2)
  {
    Serial.print("复位");
    ESP.reset();
  } 
 }else 
 {   
    check++;
    buttonTimeCheck++;
    if(buttonTimeCheck==3)
    { 
        buttonTimeCheck=0;
        check=0;
        smartConfig();
        Serial.print("配网");
    }
#ifdef debug_log
     Serial.print(check);
     Serial.print(":"); 
     Serial.print(buttonTimeCheck);   
     Serial.print("\r\n");
#endif
 }
 

  delay(1000);

#ifdef debug_log
  Serial.print("时间");
  Serial.print(time_second);
  Serial.print(":");
  Serial.print(time_minute);
  Serial.print("\n\r");
#endif

#ifdef debug_log
  Serial.print("设置时间");
  Serial.print(set_month);
  Serial.print("/");
  Serial.print(set_day);
  Serial.print("     ");
  Serial.print(set_hour);
  Serial.print(":");
  Serial.print(set_minute);
  Serial.print("  ");  
  Serial.print(set_times);
  Serial.print("\n\r");
#endif

}
 
/**
 *  时间判断
 */
void timeJudge()
{
    if (set_month==month() && set_day==day() && set_hour==hour() && set_minute==minute())
    {
        start_flag=1;
    }

    if (start_flag==1 && set_times!=time_minute)
    {
      digitalWrite(setPin,1);
      flag =~flag ;
      //digitalWrite(led_show,flag);
      digitalWrite(LED,flag); 
      Serial.print("开始");
      Serial.print("\n\r");
    }else if (start_flag==1 && set_times==time_minute)
    {
      start_flag=0;
      time_minute=0;
      time_second=0;
      digitalWrite(setPin,0);
      //digitalWrite(led_show,1);
      digitalWrite(LED,1); 
      Serial.print("结束");
      Serial.print("\n\r");
    }
    
    
}

/*-------- NTP code---------*/
 
const int NTP_PACKET_SIZE = 48; // NTP time is in the first 48 bytes of message
byte packetBuffer[NTP_PACKET_SIZE]; //buffer to hold incoming & outgoing packets
 
time_t getNtpTime()
{
  IPAddress ntpServerIP; // NTP server's ip address
 
  while (Udp.parsePacket() > 0) ; // discard any previously received packets

  //Serial.println("Transmit NTP Request");
  // get a random server from the pool
  WiFi.hostByName(ntpServerName, ntpServerIP);
#ifdef debug_log
  Serial.print(ntpServerName);
  Serial.print(": ");
  Serial.println(ntpServerIP);
#endif

  sendNTPpacket(ntpServerIP);
  uint32_t beginWait = millis();
  while (millis() - beginWait < 1500) {
    int size = Udp.parsePacket();
    if (size >= NTP_PACKET_SIZE) {
    #ifdef  debug_log
      Serial.println("Receive NTP Response");
    #endif
      Udp.read(packetBuffer, NTP_PACKET_SIZE);  // read packet into the buffer
      unsigned long secsSince1900;
      // convert four bytes starting at location 40 to a long integer
      secsSince1900 =  (unsigned long)packetBuffer[40] << 24;
      secsSince1900 |= (unsigned long)packetBuffer[41] << 16;
      secsSince1900 |= (unsigned long)packetBuffer[42] << 8;
      secsSince1900 |= (unsigned long)packetBuffer[43];
      return secsSince1900 - 2208988800UL + timeZone * SECS_PER_HOUR;
    }
  }
  Serial.println("No NTP Response :-(");
  return 0; // return 0 if unable to get the time
}
 
// send an NTP request to the time server at the given address
void sendNTPpacket(IPAddress &address)
{
  // set all bytes in the buffer to 0
  memset(packetBuffer, 0, NTP_PACKET_SIZE);
  // Initialize values needed to form NTP request
  // (see URL above for details on the packets)
  packetBuffer[0] = 0b11100011;   // LI, Version, Mode
  packetBuffer[1] = 0;     // Stratum, or type of clock
  packetBuffer[2] = 6;     // Polling Interval
  packetBuffer[3] = 0xEC;  // Peer Clock Precision
  // 8 bytes of zero for Root Delay & Root Dispersion
  packetBuffer[12] = 49;
  packetBuffer[13] = 0x4E;
  packetBuffer[14] = 49;
  packetBuffer[15] = 52;
  // all NTP fields have been given values, now
  // you can send a packet requesting a timestamp:
  Udp.beginPacket(address, 123); //NTP requests are to port 123
  Udp.write(packetBuffer, NTP_PACKET_SIZE);
  Udp.endPacket();

}
