#from dotenv import 
import os
import sqlite3
import tweepy
import time
import argparse
import socket
from confluent_kafka import Producer
from essential_generators import DocumentGenerator
import time


#load_dotenv()
api_key = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
api_key_secret = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
access_key = "3xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxL"
access_key_secret = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
bearer_token = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"


class TweetStreamV2(tweepy.StreamingClient):
      new_tweet = {}

      def on_connect(self):
            print("Connected!")
            conf = {'bootstrap.servers' : '10.8.128.121:9092',
            'client.id' : socket.gethostname()}
            self.producer = Producer(conf)

      def on_includes(self, includes):
            self.new_tweet["username"] = includes["users"][0].username
      
      def on_tweet(self, tweet):
            if tweet.referenced_tweets == None:
                  # self.new_tweet[“tweet”] = tweet.text
                  print(tweet.text)
                  self.producer.produce("test4", key = "tweet", value = tweet.text, callback=self.result)
                  self.producer.poll(1)
                  time.sleep(1)


      def result(self, err, msg):
            if err is not None:
                  print("Hatalı Gonderme : {}".format(str(err)))
            else:
                  print("Sent successfully {}".format(str(msg)))
                  print("---------------------------------")
                  print(self.new_tweet)
                  print("---------------------------------")


search_terms = ["Senol", "Gunes", "Besiktas", "Storm", "Java"]
stream = TweetStreamV2(bearer_token)

#delete previous query

# prev_id = stream.get_rules().data[0].id
# stream.delete_rules(prev_id)

for term in search_terms:
      # add new query
      stream.add_rules(tweepy.StreamRule(term))

stream.filter(tweet_fields=["created_at", "lang"], expansions=["author_id"], user_fields=["username", "name"])
#stream.filter(tweet_fields=["created_at", "lang"])


# stream.filter(
#      tweet_fields=["referenced_tweets"]
#       tweet_fields=["created_at", "lang"],
#       expansions=["author_id"],
#       user_fields=["username", "name"],
#       )