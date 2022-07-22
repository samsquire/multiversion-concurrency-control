letters = ["A", "B", "C", "D"]
numbers = ["1", "2", "3", "4"]
symbols = ["÷", "×", "(", "&"]
freq = [2, 0, 1]

class Concurrent:
  def __init__(self, sets):
    self.sets = sets
    self.N = 0
  
  def size(self):
    total = len(self.sets[0])
    for item in self.sets[1:]:
      total = total * len(item)
    return total

  def reset(self):
    self.N = 0

  def fairtick(self):
    combo = []
    N = self.N
    for index, item in enumerate(self.sets):
      N, r = divmod(N, len(item))
      combo.append(r)
    self.N = self.N + 1
    results = ""
    for index, item in enumerate(combo):
      results += self.sets[index][item]
    return results

  def biasedtick(self):
    combo = [0] * len(self.sets)
    N = self.N
    for index, item in enumerate(self.sets):
      N, r = divmod(N, len(item))
      combo[freq[index]] = r
    self.N = self.N + 1
    results = ""
    for index, item in enumerate(combo):
      results += self.sets[index][item]
    return results

print("FAIR")
cl = Concurrent([letters, numbers, symbols])
s1 = set()
for index in range(cl.size()):
  value = cl.fairtick()
  print(value)
  s1.add(value)
print("")
print("BIASED")
print("")

cl2 = Concurrent([letters, numbers, symbols])
s2 = set()
for index in range(cl2.size()):
  value = cl2.biasedtick()
  print(value)
  s2.add(value)
assert s1 == s2
