package com.isuwang.soa.monitor.api.domain;

/**
         * Autogenerated by Dapeng-Code-Generator (1.2.2)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated

        *

 平台处理数据

        **/
        public class PlatformProcessData{
        
            /**
            *

 时间间隔:单位分钟

            **/
            public int period ;
            public int getPeriod(){ return this.period; }
            public void setPeriod(int period){ this.period = period; }

            public int period(){ return this.period; }
            public PlatformProcessData period(int period){ this.period = period; return this; }
          
            /**
            *

 统计分析时间(时间戳)

            **/
            public long analysisTime ;
            public long getAnalysisTime(){ return this.analysisTime; }
            public void setAnalysisTime(long analysisTime){ this.analysisTime = analysisTime; }

            public long analysisTime(){ return this.analysisTime; }
            public PlatformProcessData analysisTime(long analysisTime){ this.analysisTime = analysisTime; return this; }
          
            /**
            *

 服务名称

            **/
            public String serviceName ;
            public String getServiceName(){ return this.serviceName; }
            public void setServiceName(String serviceName){ this.serviceName = serviceName; }

            public String serviceName(){ return this.serviceName; }
            public PlatformProcessData serviceName(String serviceName){ this.serviceName = serviceName; return this; }
          
            /**
            *

 方法名称

            **/
            public String methodName ;
            public String getMethodName(){ return this.methodName; }
            public void setMethodName(String methodName){ this.methodName = methodName; }

            public String methodName(){ return this.methodName; }
            public PlatformProcessData methodName(String methodName){ this.methodName = methodName; return this; }
          
            /**
            *

 版本号

            **/
            public String versionName ;
            public String getVersionName(){ return this.versionName; }
            public void setVersionName(String versionName){ this.versionName = versionName; }

            public String versionName(){ return this.versionName; }
            public PlatformProcessData versionName(String versionName){ this.versionName = versionName; return this; }
          
            /**
            *

 服务器IP

            **/
            public String serverIP ;
            public String getServerIP(){ return this.serverIP; }
            public void setServerIP(String serverIP){ this.serverIP = serverIP; }

            public String serverIP(){ return this.serverIP; }
            public PlatformProcessData serverIP(String serverIP){ this.serverIP = serverIP; return this; }
          
            /**
            *

 服务器端口

            **/
            public int serverPort ;
            public int getServerPort(){ return this.serverPort; }
            public void setServerPort(int serverPort){ this.serverPort = serverPort; }

            public int serverPort(){ return this.serverPort; }
            public PlatformProcessData serverPort(int serverPort){ this.serverPort = serverPort; return this; }
          
            /**
            *

 平台最小耗时(单位:毫秒)

            **/
            public long pMinTime ;
            public long getPMinTime(){ return this.pMinTime; }
            public void setPMinTime(long pMinTime){ this.pMinTime = pMinTime; }

            public long pMinTime(){ return this.pMinTime; }
            public PlatformProcessData pMinTime(long pMinTime){ this.pMinTime = pMinTime; return this; }
          
            /**
            *

 平台最大耗时(单位:毫秒)

            **/
            public long pMaxTime ;
            public long getPMaxTime(){ return this.pMaxTime; }
            public void setPMaxTime(long pMaxTime){ this.pMaxTime = pMaxTime; }

            public long pMaxTime(){ return this.pMaxTime; }
            public PlatformProcessData pMaxTime(long pMaxTime){ this.pMaxTime = pMaxTime; return this; }
          
            /**
            *

 平台平均耗时(单位:毫秒)

            **/
            public long pAverageTime ;
            public long getPAverageTime(){ return this.pAverageTime; }
            public void setPAverageTime(long pAverageTime){ this.pAverageTime = pAverageTime; }

            public long pAverageTime(){ return this.pAverageTime; }
            public PlatformProcessData pAverageTime(long pAverageTime){ this.pAverageTime = pAverageTime; return this; }
          
            /**
            *

 平台总耗时(单位:毫秒)

            **/
            public long pTotalTime ;
            public long getPTotalTime(){ return this.pTotalTime; }
            public void setPTotalTime(long pTotalTime){ this.pTotalTime = pTotalTime; }

            public long pTotalTime(){ return this.pTotalTime; }
            public PlatformProcessData pTotalTime(long pTotalTime){ this.pTotalTime = pTotalTime; return this; }
          
            /**
            *

 接口服务最小耗时(单位:毫秒)

            **/
            public long iMinTime ;
            public long getIMinTime(){ return this.iMinTime; }
            public void setIMinTime(long iMinTime){ this.iMinTime = iMinTime; }

            public long iMinTime(){ return this.iMinTime; }
            public PlatformProcessData iMinTime(long iMinTime){ this.iMinTime = iMinTime; return this; }
          
            /**
            *

 接口服务最大耗时(单位:毫秒)

            **/
            public long iMaxTime ;
            public long getIMaxTime(){ return this.iMaxTime; }
            public void setIMaxTime(long iMaxTime){ this.iMaxTime = iMaxTime; }

            public long iMaxTime(){ return this.iMaxTime; }
            public PlatformProcessData iMaxTime(long iMaxTime){ this.iMaxTime = iMaxTime; return this; }
          
            /**
            *

 接口服务平均耗时(单位:毫秒)

            **/
            public long iAverageTime ;
            public long getIAverageTime(){ return this.iAverageTime; }
            public void setIAverageTime(long iAverageTime){ this.iAverageTime = iAverageTime; }

            public long iAverageTime(){ return this.iAverageTime; }
            public PlatformProcessData iAverageTime(long iAverageTime){ this.iAverageTime = iAverageTime; return this; }
          
            /**
            *

 接口服务总耗时(单位:毫秒)

            **/
            public long iTotalTime ;
            public long getITotalTime(){ return this.iTotalTime; }
            public void setITotalTime(long iTotalTime){ this.iTotalTime = iTotalTime; }

            public long iTotalTime(){ return this.iTotalTime; }
            public PlatformProcessData iTotalTime(long iTotalTime){ this.iTotalTime = iTotalTime; return this; }
          
            /**
            *

 总调用次数

            **/
            public int totalCalls ;
            public int getTotalCalls(){ return this.totalCalls; }
            public void setTotalCalls(int totalCalls){ this.totalCalls = totalCalls; }

            public int totalCalls(){ return this.totalCalls; }
            public PlatformProcessData totalCalls(int totalCalls){ this.totalCalls = totalCalls; return this; }
          
            /**
            *

 成功调用次数

            **/
            public int succeedCalls ;
            public int getSucceedCalls(){ return this.succeedCalls; }
            public void setSucceedCalls(int succeedCalls){ this.succeedCalls = succeedCalls; }

            public int succeedCalls(){ return this.succeedCalls; }
            public PlatformProcessData succeedCalls(int succeedCalls){ this.succeedCalls = succeedCalls; return this; }
          
            /**
            *

 失败调用次数

            **/
            public int failCalls ;
            public int getFailCalls(){ return this.failCalls; }
            public void setFailCalls(int failCalls){ this.failCalls = failCalls; }

            public int failCalls(){ return this.failCalls; }
            public PlatformProcessData failCalls(int failCalls){ this.failCalls = failCalls; return this; }
          
            /**
            *

 请求的流量(单位:字节)

            **/
            public int requestFlow ;
            public int getRequestFlow(){ return this.requestFlow; }
            public void setRequestFlow(int requestFlow){ this.requestFlow = requestFlow; }

            public int requestFlow(){ return this.requestFlow; }
            public PlatformProcessData requestFlow(int requestFlow){ this.requestFlow = requestFlow; return this; }
          
            /**
            *

 响应的流量(单位:字节)

            **/
            public int responseFlow ;
            public int getResponseFlow(){ return this.responseFlow; }
            public void setResponseFlow(int responseFlow){ this.responseFlow = responseFlow; }

            public int responseFlow(){ return this.responseFlow; }
            public PlatformProcessData responseFlow(int responseFlow){ this.responseFlow = responseFlow; return this; }
          

        public String toString(){
          StringBuilder stringBuilder = new StringBuilder("{");
          stringBuilder.append("\"").append("period").append("\":").append(this.period).append(",");
    stringBuilder.append("\"").append("analysisTime").append("\":").append(this.analysisTime).append(",");
    stringBuilder.append("\"").append("serviceName").append("\":\"").append(this.serviceName).append("\",");
    stringBuilder.append("\"").append("methodName").append("\":\"").append(this.methodName).append("\",");
    stringBuilder.append("\"").append("versionName").append("\":\"").append(this.versionName).append("\",");
    stringBuilder.append("\"").append("serverIP").append("\":\"").append(this.serverIP).append("\",");
    stringBuilder.append("\"").append("serverPort").append("\":").append(this.serverPort).append(",");
    stringBuilder.append("\"").append("pMinTime").append("\":").append(this.pMinTime).append(",");
    stringBuilder.append("\"").append("pMaxTime").append("\":").append(this.pMaxTime).append(",");
    stringBuilder.append("\"").append("pAverageTime").append("\":").append(this.pAverageTime).append(",");
    stringBuilder.append("\"").append("pTotalTime").append("\":").append(this.pTotalTime).append(",");
    stringBuilder.append("\"").append("iMinTime").append("\":").append(this.iMinTime).append(",");
    stringBuilder.append("\"").append("iMaxTime").append("\":").append(this.iMaxTime).append(",");
    stringBuilder.append("\"").append("iAverageTime").append("\":").append(this.iAverageTime).append(",");
    stringBuilder.append("\"").append("iTotalTime").append("\":").append(this.iTotalTime).append(",");
    stringBuilder.append("\"").append("totalCalls").append("\":").append(this.totalCalls).append(",");
    stringBuilder.append("\"").append("succeedCalls").append("\":").append(this.succeedCalls).append(",");
    stringBuilder.append("\"").append("failCalls").append("\":").append(this.failCalls).append(",");
    stringBuilder.append("\"").append("requestFlow").append("\":").append(this.requestFlow).append(",");
    stringBuilder.append("\"").append("responseFlow").append("\":").append(this.responseFlow).append(",");
    
          stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
          stringBuilder.append("}");

          return stringBuilder.toString();
        }
      }
      