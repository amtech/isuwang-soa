package com.isuwang.dapeng.transaction

import javax.sql.DataSource

import com.isuwang.dapeng.transaction.db.dbc.DbcResources
import com.isuwang.dapeng.transaction.support.DB

/**
  * Created by tangliu on 2016/3/25.
  */
object TransactionDB extends DB {

  override def withDataSource(): DataSource = {
    DbcResources.transactionDataSorce
  }
}
