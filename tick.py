class Tick:
  def __init__(self, name, collections, func):
    self.collections = collections
    self.name = name
    self.n = 0
    self.func = func
    self.context = {}
    self.downstreams = []
    self.pending = []
    self.current = []
    for item in range(len(self.collections)):
      
      self.current.append(0)
   
  def size(self):
    if len(self.collections) == 0:
      return 0
    total = len(self.collections[0])
    for collection in self.collections[1:]:
        total = total * len(collection)
    return total

  def enqueue(self, index, item, ticker):
    if not (len(self.pending) > index):
      self.pending.append([])
    self.pending[index].append(item)
  def link(self, item):
    self.downstreams.append(item)
  def reload(self):
    self.collections = self.pending
    self.N = 0
  
  def tick(self):
    
    items = []
    n = self.n
    self.indexes = []
    
    
    # reversed to match order of itertools.product
    for collection in reversed(self.collections): 
        n, r = divmod(n, len(collection))
        self.indexes.append(r)

    
    for loop in range(len(self.collections)):
      previous = 0
      for mod in range(0, len(self.collections)):
        previous = previous + self.indexes[mod]
      
      self.indexes[loop] = previous % len(self.collections[loop])
      

    for loop, item in enumerate(self.indexes):
      items.append(self.collections[loop][item])
    
    self.n = self.n + 1
    return self.func(self, self.downstreams, items)

class MergeTick(Tick):
  def __init__(self, name, collections, func):
    super(MergeTick, self).__init__(name, collections, func)
    self.wait_size = {}
    self.waits_values = {}
    
    self.destinations = {}
    
  def wait_for(self, name, values):
    self.waits_values[name] = []
    self.wait_size[name] = len(values)
    for upstream in range(len(values)):
      self.waits_values[name].append([])
      if name not in self.destinations:
        self.destinations[name] = {}
      self.destinations[name][values[upstream]] = self.waits_values[name][upstream]

  def enqueue(self, index, item, parent):
    self.destinations[parent.name][parent].append(item)
  
  def tick(self):
    
    items = []
    for name, values in self.destinations.items():
      size = 0
      for ticker, item in values.items():
        size = size + len(item)
        if (len(item)) > 0:
          items.append(item.pop())
        if size >= self.wait_size[name]:
          return self.func(self, self.downstreams, items)
      
    

a = ["a", "b", "c"]      
b  = ["1", "2", "3"]
c = ["÷", "×", "("]

def printer(parent, downstream, items):
  output = ""
  for item in items:
    output += item
  return output

ticker = Tick("start", [a, b, c], printer)
print(ticker.size())
for index in range(ticker.size()):
  print(ticker.tick())

print("CORRECT")
for letter in a:
 for number in b:
  for symbol in c:
    print("{}{}{}".format(letter, number, symbol))

def communicator(parent, downstreams, items):
  output = ""
  for item in items:
    output += item
  for downstream in downstreams:
      downstream.enqueue(0, output, parent)
  return output


def cont(parent, downstreams, items):
  one = Tick(items[0], [b, items], communicator)
  one.link(parent.context["d"]) 
  


  two = Tick(items[0], [c, items], communicator)
  two.link(parent.context["d"])

  parent.context["d"].wait_for(items[0], [one, two])

  return [one, two]

def aggregator(parent, downstreams, items):
  output = ""
  for item in items:
    output += item
  return output
  

letter = Tick("letters", [a], cont)



downstream = MergeTick("agg", [], aggregator)
letter.context["d"] = downstream

sizes = [0]
currents = [0]
tickers = []
current = 0
tickers.append(downstream)



for index in range(letter.size()):
  
  
  for item in letter.tick():
    tickers.append(item)
    currents.append(0)
    sizes.append(item.size())
    print(item.size())
  
running = True
while running:
  running = False
  for index in range(len(tickers)):
    if currents[index] < sizes[index]:
      running = True
    if currents[index] == sizes[index]:
      tickers[index].reload()
      sizes[index] = tickers[index].size()
      currents[index] = 0
  print(tickers[current].tick())
  currents[current] = currents[current] + 1
  current = (current + 1) % len(tickers)
