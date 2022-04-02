
package com.sundogsoftware.spark

import org.apache.spark.sql._
import org.apache.log4j._
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.regression.{DecisionTreeRegressor, LinearRegression}
import org.apache.spark.sql.types._

object RealEstate {

  case class RegressionSchema(No: Integer, TransactionDate: Double,
                              HouseAge:Double,DistanceToMRT:Double,NumberConvenienceStores:Integer,
                              Latitude:Double,Longitude:Double,PriceOfUnitArea:Double
                             )

  /** Our main function where the action happens */
  def main(args: Array[String]) {
    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)

    val spark = SparkSession
      .builder
      .appName("LinearRegressionDF")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._
    val dsRaw = spark.read
      .option("sep", ",")
      .option("inferSchema",true)
      .csv("data/realestate.csv")
      .as[RegressionSchema]

    val assembler = new VectorAssembler().
      setInputCols(Array("HouseAge","DistanceToMRT","NumberConvenienceStores")).
      setOutputCol("features")
    val df = assembler.transform(dsRaw)
      .select("PriceOfUnitArea","features")

    // Let's split our data into training data and testing data
    val trainTest = df.randomSplit(Array(0.5, 0.5))
    val trainingDF = trainTest(0)
    val testDF = trainTest(1)

    // Now create our linear regression model
    val lir = new DecisionTreeRegressor().setFeaturesCol("features").setLabelCol("PriceUnitArea")



    // Train the model using our training data
    val model = lir.fit(trainingDF)

    // Now see if we can predict values in our test data.
    // Generate predictions using our linear regression model for all features in our
    // test dataframe:
    val fullPredictions = model.transform(testDF).cache()

    // This basically adds a "prediction" column to our testDF dataframe.

    // Extract the predictions and the "known" correct labels.
    val predictionAndLabel = fullPredictions.select("prediction", "PriceOfUnitArea").collect()

    // Print out the predicted and actual values for each point
    for (prediction <- predictionAndLabel) {
      println(prediction)
    }

    // Stop the session
    spark.stop()

  }
}
